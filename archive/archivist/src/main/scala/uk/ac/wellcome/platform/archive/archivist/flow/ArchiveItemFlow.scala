package uk.ac.wellcome.platform.archive.archivist.flow

import java.util.zip.ZipFile

import akka.stream.alpakka.s3.scaladsl.{MultipartUploadResult, S3Client}
import akka.stream.scaladsl.{Flow, Source}
import akka.{Done, NotUsed}
import grizzled.slf4j.Logging
import uk.ac.wellcome.platform.archive.common.models.{BagDigestItem, BagLocation}
import uk.ac.wellcome.storage.ObjectLocation

object ArchiveItemFlow extends Logging {
  def apply()(
    implicit s3Client: S3Client
  ): Flow[(BagLocation, BagDigestItem, ZipFile), Done, NotUsed] = {

    val uploadVerificationFlow = UploadVerificationFlow()
    val downloadVerification = DownloadVerificationFlow()

    Flow[(BagLocation, BagDigestItem, ZipFile)].flatMapConcat {
      case (bagLocation, bagDigestItem, zipFile) =>
        Source
          .single((bagLocation, bagDigestItem, zipFile))
          .log("uploading and verifying")
          .via(uploadVerificationFlow)
          .log("upload verified")
          .map {
            case MultipartUploadResult(_, bucket, key, _, _) =>
              (ObjectLocation(bucket, key), bagDigestItem.checksum)
          }
          .log("upload location")
          .via(downloadVerification)
          .dropWhile(_ => true)
          .log("download verified")

    }
  }
}
