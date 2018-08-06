package uk.ac.wellcome.platform.archiver.flow

import java.util.zip.ZipFile

import akka.NotUsed
import akka.stream.ActorMaterializer
import akka.stream.alpakka.s3.scaladsl.{MultipartUploadResult, S3Client}
import akka.stream.scaladsl.{Flow, Source}
import grizzled.slf4j.Logging
import uk.ac.wellcome.platform.archiver.models.BagUploaderConfig

object UploadVerificationFlow extends Logging {
  def apply(config: BagUploaderConfig)(
    implicit s3Client: S3Client,
    materializer: ActorMaterializer
  ): Flow[(BagDigestItem, ZipFile), MultipartUploadResult, NotUsed] = {

    Flow[(BagDigestItem, ZipFile)]
      .log("verifying upload")
      .flatMapConcat {
        case (BagDigestItem(checksum, location), zipFile) =>
          val extract = FileExtractorFlow()
          val verify = DigestCalculatorFlow("SHA-256", checksum)

          val uploadKey = s"${config.uploadPrefix}/${location.namespace}/${location.key}"
          val uploadSink = s3Client.multipartUpload(config.uploadNamespace, uploadKey)
          val uploadSource = Source.single((location, zipFile))
          val uploadResult = uploadSource.via(extract).via(verify).runWith(uploadSink)

          Source.fromFuture(uploadResult).log("upload result")
      }
      .log("upload verified")
  }
}
