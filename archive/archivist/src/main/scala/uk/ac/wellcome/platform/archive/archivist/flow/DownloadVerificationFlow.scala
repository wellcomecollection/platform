package uk.ac.wellcome.platform.archive.archivist.flow

import akka.Done
import akka.stream.alpakka.s3.scaladsl.S3Client
import akka.stream.scaladsl.Flow
import grizzled.slf4j.Logging
import uk.ac.wellcome.storage.ObjectLocation

object DownloadVerificationFlow extends Logging {
  def apply()(implicit s3Client: S3Client) =
    Flow[(ObjectLocation, String)]
      .log("download to verify")
      .flatMapConcat({
        case (uploadLocation, checksum) =>
          val verify = DigestCalculatorFlow("SHA-256", checksum)

          val (s3Source, _) = s3Client
            .download(uploadLocation.namespace, uploadLocation.key)

          s3Source
            .via(verify)
            .fold(Done)((out, _) => out)
      })

}
