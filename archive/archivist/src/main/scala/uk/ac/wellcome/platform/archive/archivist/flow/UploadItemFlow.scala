package uk.ac.wellcome.platform.archive.archivist.flow

import akka.NotUsed
import akka.stream.scaladsl.{Flow, Source}
import com.amazonaws.services.s3.AmazonS3
import grizzled.slf4j.Logging
import uk.ac.wellcome.platform.archive.archivist.models.{ArchiveItemJob, ZipLocation}
import uk.ac.wellcome.platform.archive.archivist.util.CompareChecksum
import uk.ac.wellcome.platform.archive.common.models.BagItem

object UploadItemFlow
  extends Logging
    with CompareChecksum {
  def apply()(
    implicit s3Client: AmazonS3
  ): Flow[ArchiveItemJob, Either[ArchiveItemJob, ArchiveItemJob],
    NotUsed] = {

    Flow[ArchiveItemJob]
      .flatMapConcat {
        case job@ArchiveItemJob(_, BagItem(checksum, _)) =>
          Source.single(job).map(ZipLocation.apply).log("extracting file from zip")
            .via(ZipFileEntryFlow.apply()).log("uploading and extracting checksum")
            .via(UploadAndGetChecksumFlow(job.uploadLocation))
            .log("comparing checksum")
            .map(compare(checksum))
            .log("to either")
            .map{
              case true => Right(job)
              case false => Left(job)
            }

      }
  }
}
