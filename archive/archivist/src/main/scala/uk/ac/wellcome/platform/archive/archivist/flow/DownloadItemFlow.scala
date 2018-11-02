package uk.ac.wellcome.platform.archive.archivist.flow

import akka.NotUsed
import akka.stream.ActorAttributes
import akka.stream.scaladsl.{Flow, Source, StreamConverters}
import com.amazonaws.services.s3.AmazonS3
import grizzled.slf4j.Logging
import uk.ac.wellcome.platform.archive.archivist.models.ArchiveItemJob
import uk.ac.wellcome.platform.archive.archivist.models.errors.ChecksumNotMatchedOnDownloadError
import uk.ac.wellcome.platform.archive.common.models.error.{
  ArchiveError,
  DownloadError
}

import scala.util.{Failure, Success, Try}

object DownloadItemFlow extends Logging {

  def apply(parallelism: Int)(implicit s3Client: AmazonS3)
    : Flow[ArchiveItemJob,
           Either[ArchiveError[ArchiveItemJob], ArchiveItemJob],
           NotUsed] = {
    Flow[ArchiveItemJob]
      .log("download to verify")
      .flatMapMerge(
        parallelism, { job =>
          val triedInputStream = Try(
            s3Client
              .getObject(job.uploadLocation.namespace, job.uploadLocation.key)
              .getObjectContent
          )

          triedInputStream match {
            case Failure(exception) =>
              warn(
                s"Failed downloading object ${job.uploadLocation} from S3",
                exception)
              Source.single(
                Left(DownloadError(exception, job.uploadLocation, job)))
            case Success(inputStream) =>
              StreamConverters
                .fromInputStream(() => inputStream)
                .via(VerifiedDownloadFlow())
                .map {
                  case calculatedChecksum
                      if job.bagDigestItem.checksum == calculatedChecksum =>
                    Right(job)
                  case calculatedChecksum =>
                    warn(s"Failed validating checksum in download for job $job")
                    Left(
                      ChecksumNotMatchedOnDownloadError(
                        expectedChecksum = job.bagDigestItem.checksum,
                        actualCheckSum = calculatedChecksum,
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
