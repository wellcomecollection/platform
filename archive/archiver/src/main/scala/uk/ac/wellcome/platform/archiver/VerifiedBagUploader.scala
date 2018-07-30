package uk.ac.wellcome.platform.archiver

import java.util.zip.ZipFile

import akka.http.scaladsl.model.Uri
import akka.stream.ActorMaterializer
import akka.stream.alpakka.s3.scaladsl.{MultipartUploadResult, S3Client}
import akka.stream.scaladsl.{Flow, Sink, Source}
import grizzled.slf4j.Logging
import uk.ac.wellcome.platform.archiver.flow.{DownloadVerificationFlow, UploadVerificationFlow}
import uk.ac.wellcome.storage.ObjectLocation

import scala.concurrent.{ExecutionContext, Future}


class VerifiedBagUploader(config: BagUploaderConfig)(
  implicit
  executionContext: ExecutionContext,
  materializer: ActorMaterializer,
  s3Client: S3Client
) extends Logging {

  import HasBagDigest._

  private val digestNames = config.digestNames
  private val uploadPrefix = config.uploadPrefix


  private def verifyAndUpload(
                               digestLocations: List[BagDigestItem],
                               zipFile: ZipFile, bagName: String,
                               uploadNamespace: String
                             ) = {

    val verifiedUploads = digestLocations.map { case BagDigestItem(checksum, bagLocation) => {

      val uploadLocation: ObjectLocation = ObjectLocation(
        uploadNamespace,
        s"$uploadPrefix/${bagLocation.namespace}/${bagLocation.key}"
      )

      val resultToLocation = Flow[Future[MultipartUploadResult]].flatMapConcat((result) => {
        Source.fromFuture(result).map {
          case MultipartUploadResult(location: Uri, bucket: String, key: String, etag: String) =>
            ObjectLocation(bucket, key)
        }
      })

      val uploadVerification = UploadVerificationFlow(zipFile, uploadLocation, checksum)
      val downloadVerification = DownloadVerificationFlow(checksum)

      val result = Source.single(bagLocation)
          .via(uploadVerification)
          .via(resultToLocation)
          .via(downloadVerification)
          .runWith(Sink.ignore)

      result.recoverWith {
        case e => Future.failed(BagUploaderError(e, bagLocation))
      }
    }}

    Future.sequence(verifiedUploads)
  }

  def verify(zipFile: ZipFile, bagName: String) = {
    val uploadSets = digestNames
      .map(digestName => ObjectLocation(bagName, digestName))
      .map(location => zipFile.getDigest(location))
      .map(_ match {
        case Left(error: MalformedBagDigestError) => Future.failed(new Exception(error.toString))
        case Right(items) => verifyAndUpload(items, zipFile, bagName, config.uploadNamespace)
      })

    Future
      .sequence(uploadSets)
      .map(_ => ())
  }
}

case class BagUploaderConfig(
                              uploadNamespace: String,
                              uploadPrefix: String = "archive",
                              digestNames: List[String] = List(
                                "manifest-md5.txt",
                                "tagmanifest-md5.txt"
                              )
                            )

case class BagIntegrityError(cause: Throwable, objectBagLocation: ObjectLocation)

case class BagUploaderError(
                             cause: Throwable,
                             objectBagLocation: ObjectLocation
                           ) extends Exception {
  override def toString: String = {
    s"""
       |Failed to upload and verify BagIt object @ ${objectBagLocation.namespace}/${objectBagLocation.key}
       |Because  "$cause".}
     """.stripMargin
  }
}

// on download verify checksums are what you set them to
