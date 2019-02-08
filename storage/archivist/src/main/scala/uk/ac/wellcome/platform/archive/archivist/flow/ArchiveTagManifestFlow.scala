package uk.ac.wellcome.platform.archive.archivist.flow

import akka.NotUsed
import akka.stream.scaladsl.Flow
import com.amazonaws.services.s3.AmazonS3
import grizzled.slf4j.Logging
import uk.ac.wellcome.platform.archive.archivist.models.errors.{ArchiveItemJobError, ArchiveJobError}
import uk.ac.wellcome.platform.archive.archivist.models.{ArchiveDigestItemJob, ArchiveItemJob, ArchiveJob}
import uk.ac.wellcome.platform.archive.common.flows.FoldEitherFlow
import uk.ac.wellcome.platform.archive.common.models.bagit.BagDigestFile
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
object ArchiveTagManifestFlow extends Logging {
  def apply(parallelism: Int)(implicit s3Client: AmazonS3)
    : Flow[ArchiveJob, Either[ArchiveError[ArchiveJob], ArchiveJob], NotUsed] =
    Flow[ArchiveJob]
      .log("archiving tag manifest")
      .map(archiveTagManifestItemJob)
      .via(UploadItemFlow(parallelism))
      .via(
        FoldEitherFlow[
          ArchiveError[ArchiveItemJob],
          (ArchiveItemJob, String),
          Either[ArchiveError[ArchiveJob], ArchiveJob]](
          ifLeft = Flow[ArchiveError[ArchiveItemJob]].map { error =>
            Left(ArchiveItemJobError(error.t.archiveJob, List(error)))
          })(
          ifRight = Flow[(ArchiveItemJob, String)]
            .map(context => archiveDigestItemJob _ tupled context)
            .via(DownloadAndVerifyDigestItemFlow(parallelism))
            .via(extractArchiveJobFlow)
        )
      )

  private def archiveTagManifestItemJob(
    archiveJob: ArchiveJob): ArchiveItemJob = {
    ArchiveItemJob(
      archiveJob = archiveJob,
      bagItemPath = archiveJob.tagManifestLocation
    )
  }

  private def archiveDigestItemJob(archiveItemJob: ArchiveItemJob,
                                   digest: String): ArchiveDigestItemJob = {
    ArchiveDigestItemJob(
      archiveJob = archiveItemJob.archiveJob,
      bagDigestItem = BagDigestFile(digest, archiveItemJob.bagItemPath)
    )
  }

  private def extractArchiveJobFlow = {
    FoldEitherFlow[
      ArchiveError[ArchiveDigestItemJob],
      ArchiveDigestItemJob,
      Either[ArchiveError[ArchiveJob], ArchiveJob]](
      ifLeft = Flow[ArchiveError[ArchiveDigestItemJob]].map { error =>
        Left(ArchiveJobError(error.t.archiveJob, List(error)))
      })(ifRight = Flow[ArchiveDigestItemJob].map { archiveDigestItemJob =>
      Right(archiveDigestItemJob.archiveJob)
    })
  }
}
