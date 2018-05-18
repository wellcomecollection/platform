package uk.ac.wellcome.storage.test.fixtures

import java.io.InputStream

import com.amazonaws.services.s3.AmazonS3
import com.twitter.inject.Logging
import io.circe.Json
import io.circe.parser.parse
import org.scalatest.concurrent.Eventually
import uk.ac.wellcome.storage.s3.{S3ClientFactory, S3ObjectLocation}
import uk.ac.wellcome.test.fixtures._

import scala.collection.JavaConverters._
import scala.util.Random

object S3 {

  case class Bucket(val name: String) {
    override def toString = s"S3.Bucket($name)"
  }

  object Bucket {
    def apply(name: String): Bucket = new Bucket(name)
  }

}

trait S3 extends Logging with Eventually {

  import S3._

  protected val localS3EndpointUrl = "http://localhost:33333"
  private val regionName = "localhost"

  protected val accessKey = "accessKey1"
  protected val secretKey = "verySecretKey1"

  def s3LocalFlags(bucket: Bucket) = s3ClientLocalFlags ++ Map(
    "aws.s3.bucketName" -> bucket.name
  )

  def s3ClientLocalFlags = Map(
    "aws.s3.endpoint" -> localS3EndpointUrl,
    "aws.s3.accessKey" -> accessKey,
    "aws.s3.secretKey" -> secretKey,
    "aws.region" -> regionName
  )

  val s3Client: AmazonS3 = S3ClientFactory.create(
    region = regionName,
    endpoint = localS3EndpointUrl,
    accessKey = accessKey,
    secretKey = secretKey
  )

  def withLocalS3Bucket[R] =
    fixture[Bucket, R](
      create = {
        val bucketName: String =
          (Random.alphanumeric take 10 mkString).toLowerCase

        s3Client.createBucket(bucketName)
        eventually { s3Client.doesBucketExistV2(bucketName) }

        Bucket(bucketName)
      },
      destroy = { bucket: Bucket =>
        safeCleanup(s3Client) {
          _.listObjects(bucket.name).getObjectSummaries.asScala.foreach {
            obj =>
              safeCleanup(obj.getKey) { s3Client.deleteObject(bucket.name, _) }
          }
        }

        s3Client.deleteBucket(bucket.name)
      }
    )

  def stringify(is: InputStream) =
    scala.io.Source.fromInputStream(is).mkString

  def getContentFromS3(s3ObjectLocation: S3ObjectLocation): String =
    getContentFromS3(s3ObjectLocation.bucket, s3ObjectLocation.key)

  def getContentFromS3(bucket: Bucket, key: String): String =
    getContentFromS3(bucket.name, key)

  def getContentFromS3(bucket: String, key: String): String =
    stringify(
      s3Client.getObject(bucket, key).getObjectContent
    )

  def getJsonFromS3(bucket: Bucket, key: String): Json = {
    parse(getContentFromS3(bucket, key)).right.get
  }

}
