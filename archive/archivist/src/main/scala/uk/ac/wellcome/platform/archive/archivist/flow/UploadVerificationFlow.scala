package uk.ac.wellcome.platform.archive.archivist.flow

import java.util.zip.ZipFile

import akka.NotUsed
import akka.stream.ActorMaterializer
import akka.stream.alpakka.s3.scaladsl.{MultipartUploadResult, S3Client}
import akka.stream.scaladsl.{Flow, Source}
import grizzled.slf4j.Logging
import uk.ac.wellcome.platform.archive.common.models.{
  BagContentItem,
  BagLocation
}
import uk.ac.wellcome.storage.ObjectLocation

object UploadVerificationFlow extends Logging {
  def apply()(
    implicit s3Client: S3Client,
    materializer: ActorMaterializer
  ): Flow[(BagLocation, BagContentItem, ZipFile),
          MultipartUploadResult,
          NotUsed] = {

    Flow[(BagLocation, BagContentItem, ZipFile)]
      .flatMapConcat {
        case (
            bagLocation,
            BagContentItem(maybeChecksum, itemLocation),
            zipFile) =>
          val uploadLocation = createUploadLocation(bagLocation, itemLocation)
          val uploadSink = s3Client.multipartUpload(
            uploadLocation.namespace,
            uploadLocation.key
          )

          val extraction = Source
            .single((itemLocation, zipFile))
            .via(FileExtractorFlow())

          val maybeVerify = maybeChecksum
            .map(DigestCalculatorFlow("SHA-256", _))
            .map(extraction.via(_))
            .getOrElse(extraction)

          val uploadResult = maybeVerify
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
        bagLocation.bagName.value,
        itemLocation.key
      ).mkString("/")
    )
}
