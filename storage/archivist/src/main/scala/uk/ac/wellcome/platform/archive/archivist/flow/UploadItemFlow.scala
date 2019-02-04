package uk.ac.wellcome.platform.archive.archivist.flow

import java.io.InputStream

import akka.NotUsed
import akka.stream.scaladsl.Flow
import com.amazonaws.services.s3.AmazonS3
import uk.ac.wellcome.platform.archive.archivist.models.errors.FileNotFoundError
import uk.ac.wellcome.platform.archive.archivist.models.{
  ArchiveItemJob,
  ZipLocation
}
import uk.ac.wellcome.platform.archive.archivist.zipfile.ZipFileReader
import uk.ac.wellcome.platform.archive.common.flows.{
  FoldEitherFlow,
  OnErrorFlow
}
import uk.ac.wellcome.platform.archive.common.models.error.ArchiveError

/** This flow extracts an item from a ZIP file, uploads it to S3 and calculates the checksum
  *
  * It emits the original archive job and the checksum.
  *
  * It returns an error if:
  *   - There's a problem getting the item from the ZIP file
  *   - The upload to S3 fails
  *
  */
object UploadItemFlow {
  def apply(parallelism: Int)(
    implicit s3Client: AmazonS3
  ): Flow[ArchiveItemJob,
          Either[ArchiveError[ArchiveItemJob], (ArchiveItemJob, String)],
          NotUsed] = {
    Flow[ArchiveItemJob]
      .map(
        archiveItemJob =>
          (
            archiveItemJob,
            ZipFileReader.maybeInputStream(
              ZipLocation(
                archiveItemJob.archiveJob.zipFile,
                archiveItemJob.bagItemPath))))
      .map {
        case (archiveItemJob, option) =>
          option
            .toRight(
              FileNotFoundError(
                archiveItemJob.bagItemPath.toString,
                archiveItemJob))
            .map(inputStream => (archiveItemJob, inputStream))
      }
      .via(
        FoldEitherFlow[
          ArchiveError[ArchiveItemJob],
          (ArchiveItemJob, InputStream),
          Either[ArchiveError[ArchiveItemJob], (ArchiveItemJob, String)]](
          ifLeft = OnErrorFlow())(ifRight = UploadInputStreamFlow(parallelism)))
  }
}
