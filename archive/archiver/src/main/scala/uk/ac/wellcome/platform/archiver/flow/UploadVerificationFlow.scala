package uk.ac.wellcome.platform.archiver.flow

import java.util.zip.ZipFile

import akka.NotUsed
import akka.stream.ActorMaterializer
import akka.stream.alpakka.s3.scaladsl.{MultipartUploadResult, S3Client}
import akka.stream.scaladsl.{Flow, Source}
import grizzled.slf4j.Logging
import uk.ac.wellcome.platform.archiver.models.BagUploaderConfig
import uk.ac.wellcome.storage.ObjectLocation

import scala.concurrent.{ExecutionContext, Future}

object UploadVerificationFlow extends Logging {
  def apply(zipFile: ZipFile, checksum: String, config: BagUploaderConfig)(
    implicit s3Client: S3Client, materializer: ActorMaterializer, executionContext: ExecutionContext
  ): Flow[ObjectLocation, Future[MultipartUploadResult], NotUsed] = {
    val verify = DigestCalculatorFlow("SHA-256", checksum)
    val extract = FileExtractorFlow(zipFile)

    Flow[ObjectLocation].map((bagLocation) => {
      val uploadLocation: ObjectLocation = ObjectLocation(
        config.uploadNamespace, s"${config.uploadPrefix}/${bagLocation.namespace}/${bagLocation.key}"
      )

      debug(s"Trying to upload to: $uploadLocation")

      val upload = s3Client.multipartUpload(uploadLocation.namespace, uploadLocation.key)
      val (_, uploadResult) = extract.via(verify).runWith(Source.single(bagLocation), upload)

      uploadResult.onComplete {
        result => debug(s"Got MultipartUploadResult: $result")
      }

      uploadResult
    })
  }
}
