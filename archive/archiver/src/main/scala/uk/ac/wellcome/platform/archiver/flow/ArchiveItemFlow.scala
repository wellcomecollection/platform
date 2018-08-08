package uk.ac.wellcome.platform.archiver.flow

import java.util.zip.ZipFile

import akka.stream.ActorMaterializer
import akka.stream.alpakka.s3.scaladsl.{MultipartUploadResult, S3Client}
import akka.stream.scaladsl.{Flow, Source}
import akka.{Done, NotUsed}
import grizzled.slf4j.Logging
import uk.ac.wellcome.platform.archiver.models.BagUploaderConfig
import uk.ac.wellcome.storage.ObjectLocation

object ArchiveItemFlow extends Logging {
  def apply(config: BagUploaderConfig)(
    implicit s3Client: S3Client,
    materializer: ActorMaterializer
  ): Flow[(BagDigestItem, ZipFile), Done, NotUsed] = {

    val uploadVerificationFlow =
      UploadVerificationFlow(config)

    val uploadLocationFlow = Flow[MultipartUploadResult]
      .map {
        case MultipartUploadResult(_, bucket, key, _, _) =>
          ObjectLocation(bucket, key)
      }

    val downloadVerification = DownloadVerificationFlow()

    Flow[(BagDigestItem, ZipFile)].flatMapConcat {
      case (bagDigestItem, zipFile) =>
        Source.single((bagDigestItem, zipFile))
          .log("uploading and verifying")
          .via(uploadVerificationFlow)
          .log("upload verified")
          .via(uploadLocationFlow)
          .log("upload location")
          .map(objectLocation => (objectLocation, bagDigestItem.checksum))
          .log("downloading to complete verification")
          .via(downloadVerification)
          .log("download verified")
    }
  }
}
