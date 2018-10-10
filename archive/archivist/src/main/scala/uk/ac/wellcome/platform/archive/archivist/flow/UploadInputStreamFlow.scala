package uk.ac.wellcome.platform.archive.archivist.flow
import java.io.InputStream

import akka.NotUsed
import akka.stream.scaladsl.{Flow, StreamConverters}
import com.amazonaws.services.s3.AmazonS3
import grizzled.slf4j.Logging
import uk.ac.wellcome.platform.archive.archivist.models.ArchiveItemJob
import uk.ac.wellcome.platform.archive.archivist.models.errors.{ChecksumNotMatchedOnUploadError, UploadError}
import uk.ac.wellcome.platform.archive.common.models.error.ArchiveError

import scala.util.{Failure, Success}

/** This flow uploads an individual item to S3, and ensures the checksum of
  * the uploaded bytes matches the checksum from the manifest.
  *
  * It emits the original archive item job.
  *
  * If the upload to S3 fails or the checksum is incorrect, it returns
  * an error instead.
  *
  */
object UploadInputStreamFlow extends Logging {
  def apply(parallelism: Int)(implicit s3Client: AmazonS3)
    : Flow[(ArchiveItemJob, InputStream),
           Either[ArchiveError[ArchiveItemJob], ArchiveItemJob],
           NotUsed] =
    Flow[(ArchiveItemJob, InputStream)]
      .log("uploading input stream and verifying checksum")
      .flatMapMerge(
        parallelism, {
          case (job, inputStream) =>
            val checksum = job.bagDigestItem.checksum
            StreamConverters
              .fromInputStream(() => inputStream)
              .log("upload bytestring")
              .via(UploadAndGetChecksumFlow(job.uploadLocation))
              .log("to either")
              .map {
                case Success(calculatedChecksum)
                    if calculatedChecksum == checksum =>
                  Right(job)
                case Success(calculatedChecksum) =>
                  warn(
                    s"Checksum didn't match: $calculatedChecksum != $checksum")
                  Left(
                    ChecksumNotMatchedOnUploadError(
                      expectedChecksum = checksum,
                      actualCheckSum = calculatedChecksum,
                      t = job
                    )
                  )
                case Failure(exception) =>
                  warn("There was an exception!", exception)
                  Left(UploadError(exception, job))
              }
        }
      )

}
