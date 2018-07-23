package uk.ac.wellcome.platform.archiver

import java.util.zip.ZipFile

import com.amazonaws.services.s3.AmazonS3ClientBuilder
import uk.ac.wellcome.platform.archiver.lib.VerifiedBagUploader
import uk.ac.wellcome.storage.s3.S3StorageBackend

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Await
import scala.concurrent.duration._

import scala.concurrent.ExecutionContext.Implicits.global


object Main {
  val s3Client = AmazonS3ClientBuilder.standard.build()
  implicit val storageBackend = new S3StorageBackend(s3Client)

  val bagName = "bag"
  val bagLocation = "/Users/k/Desktop"
  val uploadNamespace = "kennys-bucket-o-fun"

  val zipFile = new ZipFile(s"$bagLocation/$bagName.zip")

  val uploadAndVerify = new VerifiedBagUploader()
  val verifiedAndUploaded = uploadAndVerify.verify(zipFile, bagName, uploadNamespace)

  val result = Await.ready(verifiedAndUploaded, 300.seconds)
}
