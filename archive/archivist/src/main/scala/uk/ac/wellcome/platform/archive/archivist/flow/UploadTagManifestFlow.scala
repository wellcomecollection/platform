package uk.ac.wellcome.platform.archive.archivist.flow

import akka.NotUsed
import akka.stream.scaladsl.Flow
import com.amazonaws.services.s3.AmazonS3
import uk.ac.wellcome.platform.archive.archivist.models.{
  ArchiveItemJob,
  ArchiveJob
}
import uk.ac.wellcome.platform.archive.common.models.EntryPath
import grizzled.slf4j.Logging
import uk.ac.wellcome.platform.archive.common.models.error.ArchiveError

/** This flow extracts a tag manifest from a ZIP file, and uploads it to S3
  *
  * It emits the original archive job.
  *
  * It returns an error if:
  *   - There's a problem getting the item from the ZIP file
  *   - The upload to S3 fails
  *
  */
object UploadTagManifestFlow extends Logging {
  def apply(parallelism: Int)(implicit s3Client: AmazonS3)
    : Flow[ArchiveJob,
           Either[ArchiveError[ArchiveItemJob], ArchiveJob],
           NotUsed] =
    Flow[ArchiveJob]
      .log("archiving tag manifest")
      .map(archiveTagManifestItemJob)
      .via(UploadItemFlow(parallelism))
      .map(_.map(_.archiveJob))

  private def archiveTagManifestItemJob(
    archiveJob: ArchiveJob): ArchiveItemJob = {
    val tagManifestFileName = archiveJob.config.tagManifestFileName
    ArchiveItemJob(archiveJob, EntryPath(tagManifestFileName))
  }
}
