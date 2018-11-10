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
      .log("uploading input stream")
      .flatMapMerge(
        parallelism, {
          case (archiveItemJob, inputStream) =>
            StreamConverters
              .fromInputStream(() => inputStream)
              .log("upload bytestring")
              .via(S3UploadFlow(archiveItemJob.uploadLocation))
              .log("to either")
              .map {
                case Success(_) =>
                  Right(archiveItemJob)
                case Failure(exception) =>
                  warn("There was an exception!", exception)
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
