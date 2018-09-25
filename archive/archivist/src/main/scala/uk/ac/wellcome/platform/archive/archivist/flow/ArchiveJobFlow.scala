package uk.ac.wellcome.platform.archive.archivist.flow

import akka.NotUsed
import akka.stream.scaladsl.Flow
import com.amazonaws.services.s3.AmazonS3
import grizzled.slf4j.Logging
import uk.ac.wellcome.platform.archive.archivist.models.{ArchiveItemJob, ArchiveJob}

object ArchiveJobFlow extends Logging {
  def apply(delimiter: String)(
    implicit s3Client: AmazonS3
  ): Flow[ArchiveJob, Either[ArchiveItemJob, ArchiveItemJob], NotUsed] = {
    val archiveManifestFlow = ArchiveManifestFlow(delimiter)

    val uploadVerificationFlow = UploadItemFlow()
    val downloadVerification = DownloadItemFlow()

    Flow[ArchiveJob]
      .via(archiveManifestFlow)
      .log("uploading and verifying")
      .via(uploadVerificationFlow)
      .via(FoldEitherFlow[ArchiveItemJob, ArchiveItemJob, Either[ArchiveItemJob, ArchiveItemJob]](job => Left(job))(downloadVerification))
      .log("download verified")
  }
}
