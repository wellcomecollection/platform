package uk.ac.wellcome.platform.archive.archivist.flow

import java.util.zip.ZipFile

import akka.stream.ActorMaterializer
import akka.stream.alpakka.s3.scaladsl.{MultipartUploadResult, S3Client}
import akka.stream.scaladsl.{Flow, Source}
import akka.{Done, NotUsed}
import grizzled.slf4j.Logging
import uk.ac.wellcome.platform.archive.common.models.{BagContentItem, BagLocation}
import uk.ac.wellcome.storage.ObjectLocation

object ArchiveItemFlow extends Logging {
  def apply()(
    implicit s3Client: S3Client,
    materializer: ActorMaterializer
  ): Flow[(BagLocation, BagContentItem, ZipFile), Done, NotUsed] = {

    val uploadVerificationFlow = UploadVerificationFlow()
    val downloadVerification = DownloadVerificationFlow()

    Flow[(BagLocation, BagContentItem, ZipFile)].flatMapConcat {
      case (bagLocation, bagContentItem, zipFile) => {
        val archiveFlow: Source[ObjectLocation, NotUsed] = Source
          .single((bagLocation, bagContentItem, zipFile))
          .log("uploading and verifying")
          .via(uploadVerificationFlow)
          .log("upload verified")
          .map {
            case MultipartUploadResult(_, bucket, key, _, _) =>
              ObjectLocation(bucket, key)
          }
          .log("upload location")

        bagContentItem.checksum.map(checksum => {
          archiveFlow
            .map(location => (location, checksum))
            .via(downloadVerification)
            .log("download verified")
        }).getOrElse(archiveFlow.map(_ => Done))
      }
    }
  }
}
