package uk.ac.wellcome.platform.archive.archivist.flow

import akka.Done
import akka.stream.SourceShape
import akka.stream.alpakka.s3.scaladsl.S3Client
import akka.stream.scaladsl.{Flow, GraphDSL, Source, Zip}
import akka.util.ByteString
import grizzled.slf4j.Logging
import uk.ac.wellcome.storage.ObjectLocation

object DownloadVerificationFlow extends Logging with CompareChecksum {
  def apply()(implicit s3Client: S3Client) =
    Flow[(ObjectLocation, String)]
      .log("download to verify")
      .flatMapConcat({
        case (uploadLocation, checksum) =>
          val verify = DigestCalculatorFlow("SHA-256")

          val (source, _) = s3Client
            .download(uploadLocation.namespace, uploadLocation.key)

          val checkedDownload = Source.fromGraph(
            GraphDSL.create(source, verify)((_, _)) {
              implicit b =>
                (s, v) => {

                  import GraphDSL.Implicits._

                  val zip = b.add(Zip[Done, ByteString])

                  s ~> v.inlets.head

                  v.outlets(0).fold(Done)((out, _) => out) ~> zip.in0
                  v.outlets(1) ~> zip.in1

                  SourceShape(zip.out)
                }
            })

          checkedDownload.map(compare(checksum))
      })
}
