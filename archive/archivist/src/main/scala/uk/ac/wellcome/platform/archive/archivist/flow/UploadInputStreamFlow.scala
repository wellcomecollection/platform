package uk.ac.wellcome.platform.archive.archivist.flow
import java.io.InputStream

import akka.stream.scaladsl.{Flow, StreamConverters}
import com.amazonaws.services.s3.AmazonS3
import grizzled.slf4j.Logging
import uk.ac.wellcome.platform.archive.archivist.models.ArchiveItemJob

import scala.util.{Failure, Success}

object UploadInputStreamFlow extends Logging{
  def apply(parallelism: Int)(implicit s3Client: AmazonS3) =
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
                  warn(s"Checksum didn't match: $calculatedChecksum != $checksum")
                  Left(job)
                case Failure(ex) =>
                  warn("There was an exception!", ex)
                  Left(job)
              }
        }
      )

}
