package uk.ac.wellcome.platform.archive.archivist.flow

import akka.NotUsed
import akka.stream.alpakka.s3.scaladsl.S3Client
import akka.stream.scaladsl.{Flow, Source}
import grizzled.slf4j.Logging
import uk.ac.wellcome.platform.archive.archivist.models.ArchiveItemJob
import uk.ac.wellcome.platform.archive.archivist.util.CompareChecksum
import uk.ac.wellcome.platform.archive.common.models.BagItem

import scala.util.{Failure, Success}

object UploadItemFlow
  extends Logging
    with CompareChecksum {
  def apply()(
    implicit s3Client: S3Client
  ): Flow[ArchiveItemJob, Either[ArchiveItemJob, ArchiveItemJob],
    NotUsed] = {

    Flow[ArchiveItemJob]
      .flatMapConcat {
        case job@ArchiveItemJob(_, BagItem(checksum, _)) =>

          val source = Source.single(job)
          val checkedUpload = VerifiedUploadFlow(s3Client, job.uploadLocation)

          source
            .via(checkedUpload)
            .map(compare(checksum))
            .map {
              case Success(_) => Right(job)
              case Failure(_) => Left(job)
            }
      }
  }
}
