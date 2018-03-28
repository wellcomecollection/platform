package uk.ac.wellcome.test.fixtures

import com.amazonaws.auth.{AWSStaticCredentialsProvider, BasicAWSCredentials}
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration
import com.amazonaws.services.s3.model.S3ObjectSummary
import com.amazonaws.services.s3.{AmazonS3, AmazonS3ClientBuilder}
import io.circe.Json
import io.circe.parser.parse
import org.apache.commons.io.IOUtils

import scala.collection.JavaConversions._
import scala.util.Random

trait S3 {

  protected val localS3EndpointUrl = "http://localhost:33333"
  protected val regionName = "localhost"

  protected val accessKey = "accessKey1"
  protected val secretKey = "verySecretKey1"

  def s3LocalFlags(bucketName: String) = Map(
    "aws.s3.endpoint" -> localS3EndpointUrl,
    "aws.s3.accessKey" -> accessKey,
    "aws.s3.secretKey" -> secretKey,
    "aws.region" -> "localhost",
    "aws.s3.bucketName" -> bucketName
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

  def withLocalS3Bucket[R](testWith: TestWith[String, R]) = {
    val bucketName: String = (Random.alphanumeric take 10 mkString).toLowerCase

    val bucket = s3Client.createBucket(bucketName)

    try {
      testWith(bucket.getName)
    } finally {

      s3Client.listObjects(bucket.getName).getObjectSummaries.foreach {
        obj: S3ObjectSummary =>
          s3Client.deleteObject(bucket.getName, obj.getKey)
      }

      s3Client.deleteBucket(bucket.getName)
    }
  }

  def getContentFromS3(bucketName: String, key: String): String = {
    IOUtils.toString(s3Client.getObject(bucketName, key).getObjectContent)
  }

  def getJsonFromS3(bucketName: String, key: String): Json = {
    parse(getContentFromS3(bucketName, key)).right.get
  }

}
