package uk.ac.wellcome.platform.archiver

import java.io.InputStream
import java.util.zip.ZipFile

import akka.NotUsed
import akka.stream.ActorMaterializer
import akka.stream.alpakka.s3.scaladsl.S3Client
import akka.stream.scaladsl.{Flow, Sink, Source, StreamConverters}
import akka.util.ByteString
import com.amazonaws.services.s3.AmazonS3
import grizzled.slf4j.Logging
import uk.ac.wellcome.storage.ObjectLocation

import scala.concurrent.{ExecutionContext, Future}


class VerifiedBagUploader(amazonS3: AmazonS3, s3Client: S3Client, config: BagUploaderConfig)(
  implicit
  executionContext: ExecutionContext,
  materializer: ActorMaterializer
) extends Logging {

  import HasBagDigest._

  private val digestNames = config.digestNames
  private val uploadPrefix = config.uploadPrefix


  private def getStream(zipFile: ZipFile, bagLocation: ObjectLocation): InputStream =
    zipFile.getInputStream(
      zipFile.getEntry(s"${bagLocation.namespace}/${bagLocation.key}")
    )

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

      val upload = s3Client.multipartUpload(uploadLocation.namespace, uploadLocation.key)

      val download = Flow[ObjectLocation].flatMapConcat((uploadLocation) => {
        s3Client.download(uploadLocation.namespace, uploadLocation.key)._1
      })

      val verify: DigestCalculator = new DigestCalculator("MD5", checksum)

      val extract: Flow[ObjectLocation, ByteString, NotUsed] = Flow[ObjectLocation].flatMapConcat((bagLocation) => {
        StreamConverters.fromInputStream(() => {
          getStream(zipFile, bagLocation)
        })
      })

      val verified = for {
        uploadResult <- extract.via(verify).runWith(Source.single(bagLocation), upload)._2
        downloadResult <- download.via(verify).runWith(Source.single(uploadLocation), Sink.ignore)._2
      } yield (uploadResult, downloadResult)

      verified.recoverWith {
        case e => Future.failed(BagUploaderError(e, bagLocation, uploadLocation))
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
                             objectBagLocation: ObjectLocation,
                             uploadLocation: ObjectLocation
                           ) extends Exception {
  override def toString: String = {
    s"""
       |Failed to upload and verify BagIt object @ ${objectBagLocation.namespace}/${objectBagLocation.key}
       |Because  "$cause" while attempting to upload to ${uploadLocation.namespace}/${uploadLocation.key}
     """.stripMargin
  }
}

// on download verify checksums are what you set them to
