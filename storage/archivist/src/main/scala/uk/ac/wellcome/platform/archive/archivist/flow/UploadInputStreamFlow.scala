package uk.ac.wellcome.platform.archive.archivist.flow
import java.io.InputStream

import akka.NotUsed
import akka.stream.ActorAttributes
import akka.stream.scaladsl.{Flow, StreamConverters}
import com.amazonaws.services.s3.AmazonS3
import grizzled.slf4j.Logging
import uk.ac.wellcome.platform.archive.archivist.models.ArchiveItemJob
import uk.ac.wellcome.platform.archive.archivist.models.errors.UploadError
import uk.ac.wellcome.platform.archive.common.models.error.ArchiveError

import scala.util.{Failure, Success}

/** This flow uploads an individual item to S3, and calculates the checksum of
  * the uploaded bytes.
  *
  * It emits the original archive item job and the items checksum.
  *
  * If the upload to S3 fails it returns an error instead.
  *
  */
object UploadInputStreamFlow extends Logging {
  def apply(parallelism: Int)(implicit s3Client: AmazonS3)
    : Flow[(ArchiveItemJob, InputStream),
           Either[ArchiveError[ArchiveItemJob], (ArchiveItemJob, String)],
           NotUsed] =
    Flow[(ArchiveItemJob, InputStream)]
      .log("uploading input stream")
      .flatMapMerge(
        parallelism, {
          case (archiveItemJob, inputStream) =>
            StreamConverters
              .fromInputStream(() => inputStream)
              .log("upload bytestring")
              .via(UploadAndCalculateDigestFlow(archiveItemJob.uploadLocation))
              .log("to either")
              .map {
                case Success(digest) =>
                  Right((archiveItemJob, digest))
                case Failure(exception) =>
                  warn(
                    s"UploadInputStreamFlow failed with exception : ${exception.getMessage}")
                  Left(
                    UploadError(
                      archiveItemJob.uploadLocation,
                      exception,
                      archiveItemJob))
              }
        }
      )
      .withAttributes(
        ActorAttributes.dispatcher(
          "akka.stream.materializer.blocking-io-dispatcher"
        )
      )
}
