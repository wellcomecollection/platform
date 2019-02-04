package uk.ac.wellcome.platform.archive.archivist.flow

import akka.NotUsed
import akka.stream.ActorAttributes
import akka.stream.scaladsl.{Flow, Source, StreamConverters}
import com.amazonaws.services.s3.AmazonS3
import grizzled.slf4j.Logging
import uk.ac.wellcome.platform.archive.archivist.models.ArchiveDigestItemJob
import uk.ac.wellcome.platform.archive.archivist.models.errors.ChecksumNotMatchedOnDownloadError
import uk.ac.wellcome.platform.archive.common.models.error.{
  ArchiveError,
  DownloadError
}

import scala.util.{Failure, Success}

object DownloadAndVerifyDigestItemFlow extends Logging {

  import uk.ac.wellcome.platform.archive.common.ConvertibleToInputStream._

  def apply(parallelism: Int)(implicit s3Client: AmazonS3)
    : Flow[ArchiveDigestItemJob,
           Either[ArchiveError[ArchiveDigestItemJob], ArchiveDigestItemJob],
           NotUsed] = {
    Flow[ArchiveDigestItemJob]
      .log("download to verify")
      .flatMapMerge(
        parallelism, { job =>
          job.uploadLocation.toInputStream match {
            case Failure(exception) =>
              warn(
                s"Failed downloading object ${job.uploadLocation} from S3 : ${exception.getMessage}")
              Source.single(
                Left(DownloadError(exception, job.uploadLocation, job)))
            case Success(inputStream) =>
              StreamConverters
                .fromInputStream(() => inputStream)
                .via(SHA256Flow())
                .map {
                  case calculatedChecksum
                      if job.bagDigestItem.checksum == calculatedChecksum =>
                    Right(job)
                  case calculatedChecksum =>
                    warn(s"Failed checksum validation in download for job $job")
                    Left(
                      ChecksumNotMatchedOnDownloadError(
                        expectedChecksum = job.bagDigestItem.checksum,
                        actualChecksum = calculatedChecksum,
                        t = job
                      )
                    )
                }
          }
        }
      )
      .withAttributes(ActorAttributes.dispatcher(
        "akka.stream.materializer.blocking-io-dispatcher"))
  }

}
