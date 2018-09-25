package uk.ac.wellcome.platform.archive.archivist.flow

import java.io.InputStream

import akka.NotUsed
import akka.stream.scaladsl.{Flow, Source}
import com.amazonaws.services.s3.AmazonS3
import grizzled.slf4j.Logging
import uk.ac.wellcome.platform.archive.archivist.models.{ArchiveItemJob, ZipLocation}
import uk.ac.wellcome.platform.archive.archivist.zipfile.ZipFileReader
import uk.ac.wellcome.platform.archive.common.models.BagItem

object UploadItemFlow
  extends Logging {
  def apply()(
    implicit s3Client: AmazonS3
  ): Flow[ArchiveItemJob, Either[ArchiveItemJob, ArchiveItemJob],
    NotUsed] = {

    Flow[ArchiveItemJob]
      .flatMapConcat {
        case job@ArchiveItemJob(_, BagItem(checksum, _)) =>
          Source.single(job)
            .map(j => ZipFileReader.maybeInputStream(ZipLocation(j)))
            .via(FoldOptionFlow[InputStream, Either[ArchiveItemJob, ArchiveItemJob]](Left(job))(UploadInputStreamFlow(job, checksum)))
      }
  }
}
