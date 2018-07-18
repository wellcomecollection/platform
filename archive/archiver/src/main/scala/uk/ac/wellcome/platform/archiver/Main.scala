package uk.ac.wellcome.platform.archiver

import java.io.InputStream
import java.util.zip.{ZipEntry, ZipFile}

import scala.collection.JavaConverters._
import java.security.MessageDigest

import com.amazonaws.services.s3.AmazonS3ClientBuilder
import uk.ac.wellcome.storage.{ObjectLocation, StorageBackend}
import uk.ac.wellcome.storage.s3.S3StorageBackend

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}
import scala.concurrent.duration._

// Checksum validation

trait Verify[T] {
  def verify(t: T, checksum: String): Future[Unit]

  def uploadAndVerify[S <: StorageBackend](t: T, location: ObjectLocation, checksum: String)(
    implicit storageBackend: S
  ): Future[Unit]
}

object Verify {

  def apply[T](implicit v: Verify[T]): Verify[T] = v

  def verifyInputStream(inputStream: InputStream, checksum: String): Future[Unit] = {

    val byteArray = Stream
      .continually(inputStream.read)
      .takeWhile(_ != -1)
      .map(_.toByte)
      .toArray

    val computedDigest = MessageDigest
      .getInstance("MD5")
      .digest(byteArray)
      .map(0xFF & _)
      .map {
        "%02x".format(_)
      }.foldLeft("") {
      _ + _
    }

    if (checksum != computedDigest) {
      Future.failed(new Exception("Checksum validation failed!"))
    } else {
      Future.successful(())
    }
  }

  implicit class VerifyOps[T: Verify](t: T) {
    def verify(checksum: String) = Verify[T].verify(t, checksum)
    def uploadAndVerify(location: ObjectLocation, checksum: String)(implicit storageBackend: StorageBackend) =
      Verify[T].uploadAndVerify(t, location, checksum)(storageBackend)
  }

  implicit val inputStreamVerification: Verify[InputStream] =
    new Verify[InputStream] {
      def verify(inputStream: InputStream, checksum: String): Future[Unit] = verifyInputStream(inputStream,checksum)

      def uploadAndVerify[S <: StorageBackend](
                                                inputStream: InputStream,
                                                location: ObjectLocation,
                                                checksum: String
                                              )(
                                                implicit storageBackend: S
                                              ): Future[Unit] = {
        val uploadProcess = storageBackend.put(
          location,
          inputStream,
          Map(
            "md5-digest" -> checksum
          )
        )

        uploadProcess.flatMap(_ => {
          storageBackend.get(location)
        }).flatMap(verifyInputStream(_,checksum))
      }
    }
}

// Bag digest extraction

case class BagDigestItem(checksum: String, location: ObjectLocation)

trait HasBagDigest[T] {
  def getDigest(t: T, location: ObjectLocation): List[BagDigestItem]
}

object HasBagDigest {

  def apply[T](implicit d: HasBagDigest[T]): HasBagDigest[T] = d

  implicit class HasBagDigestOps[T: HasBagDigest](t: T) {
    def getDigest(location: ObjectLocation) = HasBagDigest[T].getDigest(t, location)
  }

  implicit val zipHasBagDigest: HasBagDigest[ZipFile] =
    new HasBagDigest[ZipFile] {
      def getDigest(zipFile: ZipFile, location: ObjectLocation): List[BagDigestItem] = {
        val entry = zipFile.getEntry(s"${location.namespace}/${location.key}")
        val inputStream = zipFile.getInputStream(entry)
        val string = scala.io.Source.fromInputStream(inputStream).mkString

        string
          .split("\n")
          .map(_.split("  "))
          .map {
            case Array(checksum: String, key: String) =>
              BagDigestItem(checksum, ObjectLocation(location.namespace, key))
          }.toList
      }
    }
}

// Implementation

class VerifiedBagUploader[T <: StorageBackend]()(implicit storageBackend: T) {

  import Verify._
  import HasBagDigest._

  private def digestNames = List(
    "manifest-md5.txt",
    "tagmanifest-md5.txt"
  )

  private def verifyAndUpload(digestLocations: List[BagDigestItem], zipFile: ZipFile, bagName: String, uploadNamespace: String) = {
    val verifiedUploads = digestLocations.map { case BagDigestItem(checksum, location) => {
      val zipEntry = zipFile.getEntry(s"${location.namespace}/${location.key}")
      val entryInputStream = zipFile.getInputStream(zipEntry)

      val uploadLocation: ObjectLocation = ObjectLocation(
        uploadNamespace,
        s"archive/${location.namespace}/${location.key}"
      )

      for {
        _ <- entryInputStream.verify(checksum)
        _ <- entryInputStream.uploadAndVerify(uploadLocation, checksum)
      } yield ()
    }}

    Future.sequence(verifiedUploads)
  }

  def verify(zipFile: ZipFile, bagName: String, uploadNamespace: String) = {
    val uploadSets = digestNames
      .map(digestName => ObjectLocation(bagName, digestName))
      .map(location => zipFile.getDigest(location))
      .map(verifyAndUpload(_, zipFile, bagName, uploadNamespace))

    Future
      .sequence(uploadSets)
      .map(_ => ())
  }
}

//val s3Client = AmazonS3ClientBuilder.standard.build()
//val storageBackend = new S3StorageBackend(s3Client)
//
//val bagName = "bag"
//val bagLocation = "/Users/k/Desktop"
//val uploadNamespace = "kennys-bucket-o-fun"
//
//val zipFile = new ZipFile(s"$bagLocation/$bagName.zip")
//
//val uploadAndVerify = new VerifiedBagUploader()(storageBackend)
//val verifiedAndUploaded = uploadAndVerify.verify(zipFile, bagName, uploadNamespace)
//
//val result = Await.ready(verifiedAndUploaded, 300.seconds)