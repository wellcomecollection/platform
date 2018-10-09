package uk.ac.wellcome.platform.archive.archivist.flow

import java.io.InputStream

import akka.NotUsed
import akka.stream.scaladsl.Flow
import com.amazonaws.services.s3.AmazonS3
import grizzled.slf4j.Logging
import uk.ac.wellcome.platform.archive.archivist.models.errors.{
  ArchiveError,
  FileNotFoundError
}
import uk.ac.wellcome.platform.archive.archivist.models.{
  ArchiveItemJob,
  ZipLocation
}
import uk.ac.wellcome.platform.archive.archivist.zipfile.ZipFileReader

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
          option.toRight(j).map(inputStream => (j, inputStream))
      }
      .via(
        FoldEitherFlow[
          ArchiveItemJob,
          (ArchiveItemJob, InputStream),
          Either[ArchiveError[ArchiveItemJob], ArchiveItemJob]](
          ifLeft = j => {
            warn(s"Failed extracting inputStream for $j")
            Left(FileNotFoundError(j.bagDigestItem.location.path, j))
          })(
          ifRight = UploadInputStreamFlow(parallelism))
      )
  }

}
