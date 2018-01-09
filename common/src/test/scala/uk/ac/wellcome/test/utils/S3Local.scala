package uk.ac.wellcome.test.utils

import com.amazonaws.auth.{AWSStaticCredentialsProvider, BasicAWSCredentials}
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration
import com.amazonaws.services.s3.model.S3ObjectSummary
import com.amazonaws.services.s3.{AmazonS3, AmazonS3ClientBuilder}
import com.twitter.inject.Logging
import org.scalatest.{BeforeAndAfterEach, Suite}

import scala.collection.JavaConversions._

trait S3Local extends BeforeAndAfterEach with Logging { this: Suite =>

  private val localS3EndpointUrl = "http://localhost:33333"
  private val accessKey = "accessKey1"
  private val secretKey = "verySecretKey1"
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

  def createBucketAndReturnName(bucketName: String): String = {
    s3Client.createBucket(bucketName).getName
  }

  override def beforeEach(): Unit = {
    super.beforeEach()
    val bucketList = s3Client.listBuckets()
    bucketList.foreach { bucket =>

      // This assumes we'll never put more objects in a bucket that can be retrieved
      // with a single ListObjects API call.
      s3Client.listObjects(bucket.getName).getObjectSummaries.foreach { obj: S3ObjectSummary =>
        s3Client.deleteObject(bucket.getName, obj.getKey)
      }

      s3Client.deleteBucket(bucket.getName)
      s3Client.createBucket(bucket.getName)
    }
  }
}
