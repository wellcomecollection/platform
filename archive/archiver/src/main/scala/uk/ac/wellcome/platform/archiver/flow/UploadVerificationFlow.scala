package uk.ac.wellcome.platform.archiver.flow

import java.util.zip.ZipFile

import akka.NotUsed
import akka.stream.ActorMaterializer
import akka.stream.alpakka.s3.scaladsl.{MultipartUploadResult, S3Client}
import akka.stream.scaladsl.{Flow, Source}
import grizzled.slf4j.Logging
import uk.ac.wellcome.storage.ObjectLocation

object UploadVerificationFlow extends Logging {
  def apply()(
    implicit s3Client: S3Client,
    materializer: ActorMaterializer
  ): Flow[(BagLocation, BagDigestItem, ZipFile),
          MultipartUploadResult,
          NotUsed] = {

    Flow[(BagLocation, BagDigestItem, ZipFile)]
      .flatMapConcat {
        case (bagLocation, BagDigestItem(checksum, itemLocation), zipFile) =>
          val extract = FileExtractorFlow()
          val verify = DigestCalculatorFlow("SHA-256", checksum)

          val uploadLocation = createUploadLocation(bagLocation, itemLocation)
          val uploadSink = s3Client.multipartUpload(
            uploadLocation.namespace,
            uploadLocation.key
          )

          val uploadResult =
            Source
              .single((itemLocation, zipFile))
              .via(extract)
              .via(verify)
              .runWith(uploadSink)

          Source
            .fromFuture(uploadResult)
            .log("upload result")
      }
  }

  private def createUploadLocation(
    bagLocation: BagLocation,
    itemLocation: ObjectLocation
  ) =
    ObjectLocation(
      bagLocation.storageNamespace,
      List(
        bagLocation.storagePath,
        bagLocation.bagName,
        itemLocation.key
      ).mkString("/")
    )
}

case class BagLocation(
  storageNamespace: String,
  storagePath: String,
  bagName: BagName
)
