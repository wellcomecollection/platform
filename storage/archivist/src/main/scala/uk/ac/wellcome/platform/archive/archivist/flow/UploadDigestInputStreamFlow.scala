package uk.ac.wellcome.platform.archive.archivist.flow
import java.io.InputStream

import akka.NotUsed
import akka.stream.ActorAttributes
import akka.stream.scaladsl.{Flow, StreamConverters}
import com.amazonaws.services.s3.AmazonS3
import grizzled.slf4j.Logging
import uk.ac.wellcome.platform.archive.archivist.models.{
  ArchiveDigestItemJob,
  ItemDigest
}
import uk.ac.wellcome.platform.archive.archivist.models.errors.{
  ChecksumNotMatchedOnUploadError,
  UploadDigestItemError
}
import uk.ac.wellcome.platform.archive.archivist.models.storage.ObjectMetadata
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
object UploadDigestInputStreamFlow extends Logging {
  def apply(parallelism: Int)(implicit s3Client: AmazonS3)
    : Flow[(ArchiveDigestItemJob, InputStream),
           Either[ArchiveError[ArchiveDigestItemJob], ArchiveDigestItemJob],
           NotUsed] =
    Flow[(ArchiveDigestItemJob, InputStream)]
      .log("uploading input stream and verifying checksum")
      .flatMapMerge(
        parallelism, {
          case (job, inputStream) =>
            val digest = ItemDigest(job.bagDigestItem.checksum)
            StreamConverters
              .fromInputStream(() => inputStream)
              .log("upload bytestring")
              .via(UploadAndCalculateDigestFlow(
                job.uploadLocation,
                uploadMetadata(digest)))
              .log("to either")
              .map {
                case Success(calculatedDigest)
                    if calculatedDigest == digest.value =>
                  Right(job)
                case Success(calculatedDigest) =>
                  warn(s"Digests didn't match: $calculatedDigest != $digest")
                  Left(
                    ChecksumNotMatchedOnUploadError(
                      expectedChecksum = digest.value,
                      actualChecksum = calculatedDigest,
                      t = job
                    )
                  )
                case Failure(exception) =>
                  warn(
                    s"UploadDigestInputStreamFlow failed with exception : ${exception.getMessage}")
                  Left(UploadDigestItemError(exception, job))
              }
        }
      )
      .withAttributes(
        ActorAttributes.dispatcher(
          "akka.stream.materializer.blocking-io-dispatcher"
        )
      )

  def uploadMetadata(digest: ItemDigest): Option[ObjectMetadata] = {
    Some(
      ObjectMetadata(
        userMetadata = Map(digest.algorithm.toString -> digest.value)))
  }

}
