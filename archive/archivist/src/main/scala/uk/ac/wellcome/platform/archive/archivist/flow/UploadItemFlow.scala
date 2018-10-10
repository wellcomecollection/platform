package uk.ac.wellcome.platform.archive.archivist.flow

import java.io.InputStream

import akka.NotUsed
import akka.stream.scaladsl.Flow
import com.amazonaws.services.s3.AmazonS3
import grizzled.slf4j.Logging
import uk.ac.wellcome.platform.archive.archivist.models.errors.FileNotFoundError
import uk.ac.wellcome.platform.archive.archivist.models.{ArchiveItemJob, ZipLocation}
import uk.ac.wellcome.platform.archive.archivist.zipfile.ZipFileReader
import uk.ac.wellcome.platform.archive.common.flows.{FoldEitherFlow, OnErrorFlow}
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
object UploadItemFlow extends Logging {
  def apply(parallelism: Int)(
    implicit s3Client: AmazonS3
  ): Flow[ArchiveItemJob,
          Either[ArchiveError[ArchiveItemJob], ArchiveItemJob],
          NotUsed] = {

    Flow[ArchiveItemJob]
      .map(j => (j, ZipFileReader.maybeInputStream(ZipLocation(j))))
      .map {
        case (j, option) =>
          option.toRight(FileNotFoundError(j.bagDigestItem.location.path, j)).map(inputStream => (j, inputStream))
      }
      .via(
        FoldEitherFlow[
          ArchiveError[ArchiveItemJob],
          (ArchiveItemJob, InputStream),
          Either[ArchiveError[ArchiveItemJob], ArchiveItemJob]](OnErrorFlow())(UploadInputStreamFlow(parallelism)))
  }

}
