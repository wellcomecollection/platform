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

  val bucketName: String

  // These are the default access/secret keys for the scality/s3 Docker image
  // we use.  See http://s3-server.readthedocs.io/en/latest/GETTING_STARTED/
  //
  // Because this is the only image that cares about exact access keys,
  // s3LocalFlags should be added _last_.
  //
  // TODO: Make this less fragile.
  private val accessKey = "accessKey1"
  private val secretKey = "verySecretKey1"

  val s3LocalFlags: Map[String, String] =
    Map(
      "aws.s3.endpoint" -> localS3EndpointUrl,
      "aws.s3.accessKey" -> accessKey,
      "aws.s3.secretKey" -> secretKey,
      "aws.region" -> "localhost"
    )

  val credentials = new AWSStaticCredentialsProvider(
    new BasicAWSCredentials(accessKey, secretKey))

  val s3Client: AmazonS3 = AmazonS3ClientBuilder
    .standard()
    .withPathStyleAccessEnabled(true)
    .withCredentials(credentials)
    .withEndpointConfiguration(
      new EndpointConfiguration(localS3EndpointUrl, "localhost"))
    .build()

  s3Client.createBucket(bucketName)

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
    }
  }

  def getContentFromS3(bucketName: String, key: String): String = {
    IOUtils.toString(s3Client.getObject(bucketName, key).getObjectContent)
  }

  def getJsonFromS3(bucketName: String, key: String): Json = {
    parse(getContentFromS3(bucketName, key)).right.get
  }
}
