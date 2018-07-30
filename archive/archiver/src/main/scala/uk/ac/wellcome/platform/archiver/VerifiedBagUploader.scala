package uk.ac.wellcome.platform.archiver

import java.io.InputStream
import java.util.zip.ZipFile

import akka.stream.alpakka.s3.scaladsl.{MultipartUploadResult, S3Client}
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, RunnableGraph, Sink, Source, StreamConverters}
import akka.stream.{ActorMaterializer, ClosedShape}
import akka.util.ByteString
import com.amazonaws.services.s3.AmazonS3
import grizzled.slf4j.Logging
import uk.ac.wellcome.storage.ObjectLocation

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try


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

      // upload

      val uploadLocation: ObjectLocation = ObjectLocation(
        uploadNamespace,
        s"$uploadPrefix/${bagLocation.namespace}/${bagLocation.key}"
      )

      val s3Sink: Sink[ByteString, Future[MultipartUploadResult]] = s3Client.multipartUpload(
        uploadLocation.namespace,
        uploadLocation.key
      )

      val verifySink = Sink.head[Try[String]]

      val uploadAndVerify = RunnableGraph.fromGraph(GraphDSL.create(s3Sink, verifySink)((_,_)) { implicit builder =>
        (aSink, bSink) =>

        import GraphDSL.Implicits._

        val in = StreamConverters.fromInputStream(() => getStream(zipFile, bagLocation))
        val digestCalculator = new DigestCalculator("MD5", checksum)

        val broadcast = builder.add(Broadcast[ByteString](2))

        in ~> broadcast ~> aSink
        broadcast ~> digestCalculator ~> bSink

        ClosedShape
      })

      // download

      val verifySink2 = Sink.head[Try[String]]

      info(s"${uploadLocation.namespace}, ${uploadLocation.key}")


      val downloadVerifier = Flow[ObjectLocation].flatMapConcat((uploadLocation) => {
        val (source, _) = s3Client.download(uploadLocation.namespace, uploadLocation.key)

        source
      }).via(new DigestCalculator("MD5", checksum))



      val (futureResult, futureVerification) = uploadAndVerify.run()

      val verified = for {
        result <- futureResult
        _ = info(s"Uploaded file to S3 @ ${result.bucket}/${result.key}")
        verification <- futureVerification

        if verification.isSuccess
        _ = info(s"Verified file contents in zip for ${result.key}")

        downloadVerification <- Source.single(uploadLocation).via(downloadVerifier).runWith(verifySink2)
        if downloadVerification.isSuccess
        _ = info(s"Verified file contents in storage for ${result.key}.")

      } yield result

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
                                //"manifest-md5.txt",
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
