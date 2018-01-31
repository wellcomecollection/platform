package uk.ac.wellcome.test.utils

import com.amazonaws.auth.{AWSStaticCredentialsProvider, BasicAWSCredentials}
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration
import com.amazonaws.services.s3.model.S3ObjectSummary
import com.amazonaws.services.s3.{AmazonS3, AmazonS3ClientBuilder}
import com.twitter.inject.Logging
import io.circe.Json
import io.circe.parser.parse
import org.apache.commons.io.IOUtils
import org.scalatest.{BeforeAndAfterEach, Suite}

import scala.collection.JavaConversions._

trait S3Local extends BeforeAndAfterEach with Logging { this: Suite =>

  private val localS3EndpointUrl = "http://localhost:33333"
  private val accessKey = "accessKey1"
  private val secretKey = "verySecretKey1"

  val bucketName: String

  val s3LocalFlags: Map[String, String] =
    Map(
      "aws.s3.endpoint" -> localS3EndpointUrl,
      "aws.accessKey" -> accessKey,
      "aws.secretKey" -> secretKey,
      "aws.region" -> "eu-west-1"
    )

  val credentials = new AWSStaticCredentialsProvider(
    new BasicAWSCredentials(accessKey, secretKey))

  val s3Client: AmazonS3 = AmazonS3ClientBuilder
    .standard()
    .withPathStyleAccessEnabled(true)
    .withCredentials(credentials)
    .withEndpointConfiguration(
      new EndpointConfiguration(localS3EndpointUrl, "eu-west-1"))
    .build()

  override def beforeEach(): Unit = {
    super.beforeEach()
    val bucketList = s3Client.listBuckets()
    bucketList.foreach { bucket =>
      // This assumes we'll never put more objects in a bucket that can be retrieved
      // with a single ListObjects API call.
      s3Client.listObjects(bucket.getName).getObjectSummaries.foreach {
        obj: S3ObjectSummary =>
          s3Client.deleteObject(bucket.getName, obj.getKey)
      }

      s3Client.deleteBucket(bucket.getName)
      s3Client.createBucket(bucket.getName)
    }

    s3Client.createBucket(bucketName)
  }

  def getContentFromS3(bucketName: String, key: String): String = {
    IOUtils.toString(s3Client.getObject(bucketName, key).getObjectContent)
  }

  def getJsonFromS3(bucketName: String, key: String): Json = {
    parse(getContentFromS3(bucketName, key)).right.get
  }
}
