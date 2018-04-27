package uk.ac.wellcome.storage.test.fixtures

import com.amazonaws.auth.{AWSStaticCredentialsProvider, BasicAWSCredentials}
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration
import com.amazonaws.services.s3.{AmazonS3, AmazonS3ClientBuilder}
import com.twitter.inject.Logging
import io.circe.{Decoder, Json}
import io.circe.parser.parse
import org.apache.commons.io.IOUtils
import org.scalatest.Matchers
import org.scalatest.concurrent.Eventually
import uk.ac.wellcome.message.MessagePointer
import uk.ac.wellcome.utils.JsonUtil
import uk.ac.wellcome.test.fixtures._

import scala.collection.JavaConversions._
import scala.util.{Random, Success, Try}
import uk.ac.wellcome.utils.JsonUtil._

object S3 {

  class Bucket(val name: String) extends AnyVal {
    override def toString = s"S3.Bucket($name)"
  }

  object Bucket {
    def apply(name: String): Bucket = new Bucket(name)
  }

}

trait S3 extends Logging with Eventually with Matchers with ImplicitLogging {

  import S3._

  protected val localS3EndpointUrl = "http://localhost:33333"
  protected val regionName = "localhost"

  protected val accessKey = "accessKey1"
  protected val secretKey = "verySecretKey1"

  def s3LocalFlags(bucket: Bucket) = Map(
    "aws.s3.endpoint" -> localS3EndpointUrl,
    "aws.s3.accessKey" -> accessKey,
    "aws.s3.secretKey" -> secretKey,
    "aws.region" -> "localhost",
    "aws.s3.bucketName" -> bucket.name
  )

  private val credentials = new AWSStaticCredentialsProvider(
    new BasicAWSCredentials(accessKey, secretKey))

  val s3Client: AmazonS3 = AmazonS3ClientBuilder
    .standard()
    .withPathStyleAccessEnabled(true)
    .withCredentials(credentials)
    .withEndpointConfiguration(
      new EndpointConfiguration(localS3EndpointUrl, regionName))
    .build()

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
          _.listObjects(bucket.name).getObjectSummaries.foreach { obj =>
            safeCleanup(obj.getKey) { s3Client.deleteObject(bucket.name, _) }
          }
        }

        s3Client.deleteBucket(bucket.name)
      }
    )

  def getContentFromS3(bucket: Bucket, key: String): String = {
    IOUtils.toString(s3Client.getObject(bucket.name, key).getObjectContent)
  }

  def getObjectFromS3[T](snsMessage: MessageInfo)(implicit decoder: Decoder[T]): T = {
    val tryMessagePointer = fromJson[MessagePointer](snsMessage.message)
    tryMessagePointer shouldBe a[Success[_]]

    val messagePointer = tryMessagePointer.get

    val tryT = fromJson[T](getContentFromS3(Bucket(messagePointer.src.bucket), messagePointer.src.key))
    tryT shouldBe a[Success[_]]

    tryT.get
  }

  def getJsonFromS3(bucket: Bucket, key: String): Json = {
    parse(getContentFromS3(bucket, key)).right.get
  }

}
