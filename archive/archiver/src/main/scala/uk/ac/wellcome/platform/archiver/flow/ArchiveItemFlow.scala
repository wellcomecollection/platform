package uk.ac.wellcome.platform.archiver.flow

import java.util.zip.ZipFile

import akka.stream.ActorMaterializer
import akka.stream.alpakka.s3.scaladsl.{MultipartUploadResult, S3Client}
import akka.stream.scaladsl.{Flow, Source}
import akka.{Done, NotUsed}
import grizzled.slf4j.Logging
import uk.ac.wellcome.platform.archiver.models.BagUploaderConfig
import uk.ac.wellcome.storage.ObjectLocation

import scala.concurrent.{ExecutionContext, Future}


object ArchiveItemFlow extends Logging {
  def apply(zipFile:ZipFile, config: BagUploaderConfig)(
    implicit s3Client: S3Client, materializer: ActorMaterializer, executionContext: ExecutionContext
  ): Flow[BagDigestItem, Future[Done], NotUsed] = {

    Flow[BagDigestItem].flatMapConcat(bagDigestItem => {

      val resultToLocation = Flow[Future[MultipartUploadResult]].flatMapConcat((result) => {
        Source.fromFuture(result).map {
          case MultipartUploadResult(_, bucket, key, _) => {
            val uploadLocation = ObjectLocation(bucket, key)

            debug(s"Uploaded to: $uploadLocation")

            uploadLocation
          }
        }
      })

      val uploadVerification = UploadVerificationFlow(zipFile, bagDigestItem.checksum, config)
      val downloadVerification = DownloadVerificationFlow(bagDigestItem.checksum)

      Source.single(bagDigestItem.location)
        .via(uploadVerification)
        .via(resultToLocation)
        .via(downloadVerification)

    })
  }
}