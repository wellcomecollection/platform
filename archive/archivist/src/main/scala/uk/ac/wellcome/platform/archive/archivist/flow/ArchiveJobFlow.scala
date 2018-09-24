package uk.ac.wellcome.platform.archive.archivist.flow

import akka.stream.scaladsl.Flow
import com.amazonaws.services.s3.AmazonS3
import grizzled.slf4j.Logging
import uk.ac.wellcome.platform.archive.archivist.models.ArchiveJob

object ArchiveJobFlow extends Logging {
  def apply(delimiter: String)(
    implicit s3Client: AmazonS3
  ) = {
    val archiveManifestFlow = ArchiveManifestFlow(delimiter)

    val uploadVerificationFlow = UploadItemFlow()
    val downloadVerification = DownloadItemFlow()

    Flow[ArchiveJob]
      .via(archiveManifestFlow)
      .log("uploading and verifying")
      .via(uploadVerificationFlow)
      .log("verifying download")
      .via(downloadVerification)
      .log("download verified")
  }
}
