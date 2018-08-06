package uk.ac.wellcome.platform.archiver.flow

import akka.stream.Materializer
import akka.stream.alpakka.s3.scaladsl.S3Client
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.{Done, NotUsed}
import grizzled.slf4j.Logging
import uk.ac.wellcome.storage.ObjectLocation

object DownloadVerificationFlow extends Logging {
  def apply()(implicit s3Client: S3Client, materializer: Materializer)
    : Flow[(ObjectLocation, String), Done, NotUsed] = {
    Flow[(ObjectLocation, String)]
      .log("download to verify")
      .flatMapConcat({
        case (uploadLocation, checksum) =>
          val verify = DigestCalculatorFlow("SHA-256", checksum)
          Source.fromFuture(
            s3Client
              .download(uploadLocation.namespace, uploadLocation.key)
              ._1
              .via(verify)
              .runWith(Sink.ignore))
      })
      .log("download verified")
  }
}
