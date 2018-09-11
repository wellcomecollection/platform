package uk.ac.wellcome.platform.archive.archivist.flow

import java.util.zip.ZipFile

import akka.NotUsed
import akka.stream.FlowShape
import akka.stream.alpakka.s3.scaladsl.{MultipartUploadResult, S3Client}
import akka.stream.scaladsl.{Flow, GraphDSL, Source}
import grizzled.slf4j.Logging
import uk.ac.wellcome.platform.archive.common.models.{BagDigestItem, BagLocation}
import uk.ac.wellcome.storage.ObjectLocation

object UploadVerificationFlow extends Logging {
  def apply()(
    implicit s3Client: S3Client
  ): Flow[(BagLocation, BagDigestItem, ZipFile),
    MultipartUploadResult,
    NotUsed] = {

    Flow[(BagLocation, BagDigestItem, ZipFile)]
      .flatMapConcat {
        case (bagLocation, BagDigestItem(checksum, itemLocation), zipFile) =>
          val extract = FileExtractorFlow()
          val verify = DigestCalculatorFlow("SHA-256", checksum)

          val uploadLocation = createUploadLocation(bagLocation, itemLocation)
          val uploadFlow = createS3UploadFlow(s3Client, uploadLocation)

          Source
            .single((itemLocation, zipFile))
            .via(extract)
            .via(verify)
            .via(uploadFlow)
            .log("upload result")
      }
  }

  private def createS3UploadFlow(s3Client: S3Client, objectLocation: ObjectLocation) = {
    val s3Sink = s3Client.multipartUpload(
      objectLocation.namespace,
      objectLocation.key
    )

    Flow.fromGraph(GraphDSL.create(s3Sink) { implicit builder =>
      sink =>
        FlowShape(sink.in, builder.materializedValue)
    }).flatMapConcat(Source.fromFuture)
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
