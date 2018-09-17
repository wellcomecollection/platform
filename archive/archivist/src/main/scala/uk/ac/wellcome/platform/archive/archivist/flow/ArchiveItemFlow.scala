package uk.ac.wellcome.platform.archive.archivist.flow

import akka.NotUsed
import akka.stream.alpakka.s3.scaladsl.S3Client
import akka.stream.scaladsl.Flow
import grizzled.slf4j.Logging

object ArchiveItemFlow extends Logging {
  def apply()(
    implicit s3Client: S3Client
  ) = {
    val uploadVerificationFlow = UploadVerificationFlow()
    val downloadVerification = DownloadVerificationFlow()

    Flow[ArchiveItemJob]
      .log("uploading and verifying")
      .via(uploadVerificationFlow)
      .log("upload verified")
      .via(downloadVerification)
      .log("download verified")
  }
}
