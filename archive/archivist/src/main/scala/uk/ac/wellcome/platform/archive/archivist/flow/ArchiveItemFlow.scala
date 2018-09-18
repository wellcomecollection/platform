package uk.ac.wellcome.platform.archive.archivist.flow

import akka.stream.alpakka.s3.scaladsl.S3Client
import akka.stream.scaladsl.Flow
import grizzled.slf4j.Logging
import uk.ac.wellcome.platform.archive.archivist.models.{ArchiveItemJob, ArchiveJob}

object ArchiveItemFlow extends Logging {
  def apply(delimiter: String)(
    implicit s3Client: S3Client
  ) = {
    val bagDigestItemFlow = BagDigestItemFlow(delimiter)

    val uploadVerificationFlow = UploadItemFlow()
    val downloadVerification = DownloadItemFlow()

    Flow[ArchiveJob]
      .via(bagDigestItemFlow)
      .log("uploading and verifying")
      .via(uploadVerificationFlow)
      .log("upload verified")
      .via(downloadVerification)
      .log("download verified")
  }
}
