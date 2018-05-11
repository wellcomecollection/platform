package uk.ac.wellcome.storage.test.fixtures

import com.amazonaws.services.s3.AmazonS3
import com.twitter.inject.Logging
import io.circe.Json
import io.circe.parser.parse
import org.apache.commons.io.IOUtils
import org.scalatest.concurrent.Eventually
import uk.ac.wellcome.models.aws.AWSConfig
import uk.ac.wellcome.storage.s3.S3ClientModule
import uk.ac.wellcome.test.fixtures._

import scala.collection.JavaConverters._
import scala.util.Random

object S3 {

  class Bucket(val name: String) extends AnyVal {
    override def toString = s"S3.Bucket($name)"
  }

  object Bucket {
    def apply(name: String): Bucket = new Bucket(name)
  }

}

trait S3 extends Logging with Eventually {

  import S3._

  protected val localS3EndpointUrl = "http://localhost:33333"
  protected val regionName = "localhost"

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

  val s3Client: AmazonS3 = S3ClientModule.buildS3Client(
    awsConfig = AWSConfig(region = regionName),
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

  def getContentFromS3(bucket: Bucket, key: String): String = {
    IOUtils.toString(s3Client.getObject(bucket.name, key).getObjectContent)
  }

  def getJsonFromS3(bucket: Bucket, key: String): Json = {
    parse(getContentFromS3(bucket, key)).right.get
  }

}
