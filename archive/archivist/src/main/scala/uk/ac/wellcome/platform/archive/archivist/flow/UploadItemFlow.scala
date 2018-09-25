package uk.ac.wellcome.platform.archive.archivist.flow

import java.io.InputStream

import akka.NotUsed
import akka.stream.scaladsl.Flow
import com.amazonaws.services.s3.AmazonS3
import grizzled.slf4j.Logging
import uk.ac.wellcome.platform.archive.archivist.models.{ArchiveItemJob, ZipLocation}
import uk.ac.wellcome.platform.archive.archivist.zipfile.ZipFileReader

object UploadItemFlow
  extends Logging {
  def apply()(
    implicit s3Client: AmazonS3
  ): Flow[ArchiveItemJob, Either[ArchiveItemJob, ArchiveItemJob],
    NotUsed] = {

    Flow[ArchiveItemJob]
      .map(j => (j, ZipFileReader.maybeInputStream(ZipLocation(j))))
      .map {
      case (j, Some(inputStream)) => Right((j, inputStream))
      case (j, None)              => Left(j)
    }
    .via(
        FoldEitherFlow[
          ArchiveItemJob,
          (ArchiveItemJob, InputStream),
          Either[ArchiveItemJob, ArchiveItemJob]](j => Left(j))(
          UploadInputStreamFlow()))
  }

}
