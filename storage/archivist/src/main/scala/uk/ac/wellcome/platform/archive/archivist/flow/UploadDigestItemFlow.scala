package uk.ac.wellcome.platform.archive.archivist.flow

import java.io.InputStream

import akka.NotUsed
import akka.stream.scaladsl.Flow
import com.amazonaws.services.s3.AmazonS3
import uk.ac.wellcome.platform.archive.archivist.models.errors.FileNotFoundError
import uk.ac.wellcome.platform.archive.archivist.models.{
  ArchiveDigestItemJob,
  ZipLocation
}
import uk.ac.wellcome.platform.archive.archivist.zipfile.ZipFileReader
import uk.ac.wellcome.platform.archive.common.flows.{
  FoldEitherFlow,
  OnErrorFlow
}
import uk.ac.wellcome.platform.archive.common.models.error.ArchiveError

/** This flow extracts an item from a ZIP file, uploads it to S3 and validates
  * the checksum matches the manifest.
  *
  * It emits the original archive item job.
  *
  * It returns an error if:
  *   - There's a problem getting the item from the ZIP file
  *   - The upload to S3 fails
  *   - The checksums don't match
  *
  */
object UploadDigestItemFlow {
  def apply(parallelism: Int)(
    implicit s3Client: AmazonS3
  ): Flow[ArchiveDigestItemJob,
          Either[ArchiveError[ArchiveDigestItemJob], ArchiveDigestItemJob],
          NotUsed] = {

    Flow[ArchiveDigestItemJob]
      .map(job => (job, ZipFileReader.maybeInputStream(ZipLocation(job))))
      .map {
        case (job, option) =>
          option
            .toRight(FileNotFoundError(job.bagDigestItem.path.toString, job))
            .map(inputStream => (job, inputStream))
      }
      .via(
        FoldEitherFlow[
          ArchiveError[ArchiveDigestItemJob],
          (ArchiveDigestItemJob, InputStream),
          Either[ArchiveError[ArchiveDigestItemJob], ArchiveDigestItemJob]](
          OnErrorFlow())(UploadDigestInputStreamFlow(parallelism)))
  }

}
