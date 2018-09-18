package uk.ac.wellcome.platform.archive.archivist.streams.flow

import akka.stream.alpakka.s3.scaladsl.S3Client
import akka.stream.scaladsl.Flow
import grizzled.slf4j.Logging
import uk.ac.wellcome.platform.archive.archivist.models.ArchiveItemJob

object ArchiveItemFlow extends Logging {
  def apply()(
    implicit s3Client: S3Client
  ) = {
    val uploadVerificationFlow = UploadItemFlow()
    val downloadVerification = DownloadItemFlow()

    Flow[ArchiveItemJob]
      .log("uploading and verifying")
      .via(uploadVerificationFlow)
      .log("upload verified")
      .via(downloadVerification)
      .log("download verified")
  }
}
