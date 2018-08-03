package uk.ac.wellcome.platform.archiver.flow

import akka.NotUsed
import akka.stream.alpakka.s3.scaladsl.S3Client
import akka.stream.scaladsl.Flow
import akka.util.ByteString
import grizzled.slf4j.Logging
import uk.ac.wellcome.storage.ObjectLocation

object DownloadVerificationFlow extends Logging {
  def apply()(implicit s3Client: S3Client): Flow[(ObjectLocation, String), ByteString, NotUsed] = {
    Flow[(ObjectLocation, String)].flatMapConcat { case(uploadLocation, checksum) =>
      val verify = DigestCalculatorFlow("SHA-256", checksum)

      debug(s"Downloading $uploadLocation.")

      s3Client.download(uploadLocation.namespace, uploadLocation.key)._1.via(verify)
    }
  }
}
