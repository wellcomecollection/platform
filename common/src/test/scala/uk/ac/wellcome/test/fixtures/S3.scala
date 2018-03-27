package uk.ac.wellcome.test.fixtures

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, Materializer}
import akka.stream.alpakka.s3.{MemoryBufferType, S3Settings}
import akka.stream.alpakka.s3.scaladsl.S3Client
import com.amazonaws.auth.{
  AWSStaticCredentialsProvider,
  BasicAWSCredentials,
  DefaultAWSCredentialsProviderChain
}
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration
import com.amazonaws.regions.AwsRegionProvider
import com.amazonaws.services.s3.model.S3ObjectSummary
import com.amazonaws.services.s3.{AmazonS3, AmazonS3ClientBuilder}
import com.twitter.inject.Logging
import io.circe.Json
import io.circe.parser.parse
import org.apache.commons.io.IOUtils
import org.scalatest.concurrent.Eventually
import com.amazonaws.services.s3.model.ObjectMetadata

import scala.collection.JavaConversions._
import scala.util.{Random, Try}
import java.nio.file.Paths
import java.net.URL

trait S3 extends Logging with Eventually {

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
      new EndpointConfiguration(localS3EndpointUrl, regionName))
    .build()

  def withLocalS3Bucket[R](testWith: TestWith[String, R]) = {
    val bucketName: String = (Random.alphanumeric take 10 mkString).toLowerCase

    val bucket = s3Client.createBucket(bucketName)
    eventually { s3Client.doesBucketExistV2(bucketName) }

    try {
      testWith(bucket.getName)
    } finally {

      safeCleanup(s3Client) {
        _.listObjects(bucket.getName).getObjectSummaries.foreach { obj =>
          safeCleanup(obj.getKey) { s3Client.deleteObject(bucket.getName, _) }
        }
      }

      safeCleanup(bucket.getName) { s3Client.deleteBucket(_) }
    }
  }

  def withLocalS3ObjectFromResource[R](bucketName: String, resource: URL)(
    testWith: TestWith[String, R]) = {
    val metadata = new ObjectMetadata()
    val key = Paths.get(resource.toURI).getFileName.toString

    s3Client.putObject(bucketName, key, resource.openStream(), metadata)

    try {
      testWith(key)
    } finally {
      safeCleanup(key) { s3Client.deleteObject(bucketName, _) }
    }
  }

  def safeCleanup[T](resource: T)(f: T => Unit): Unit = {
    Try {
      logger.debug(s"cleaning up resource=[$resource]")
      f(resource)
    } recover {
      case e =>
        logger.warn(s"error cleaning up resource=[$resource]")
        e.printStackTrace()
    }
  }

  def getContentFromS3(bucketName: String, key: String): String = {
    IOUtils.toString(s3Client.getObject(bucketName, key).getObjectContent)
  }

  def getJsonFromS3(bucketName: String, key: String): Json = {
    parse(getContentFromS3(bucketName, key)).right.get
  }

}

trait S3AkkaClient extends S3 with AkkaFixtures {
  def withS3AkkaClient[R](
    actorSystem: ActorSystem,
    materializer: Materializer)(testWith: TestWith[S3Client, R]): R = {

    logger.debug(s"creating S3 Akka client pointing to=[$localS3EndpointUrl]")
    val s3AkkaClient = new S3Client(
      new S3Settings(
        bufferType = MemoryBufferType,
        proxy = None,
        credentialsProvider = new AWSStaticCredentialsProvider(
          new BasicAWSCredentials(accessKey, secretKey)
        ),
        s3RegionProvider = new AwsRegionProvider {
          def getRegion: String = regionName
        },
        pathStyleAccess = true,
        endpointUrl = Some(localS3EndpointUrl)
      )
    )(actorSystem, materializer)

    testWith(s3AkkaClient)

  }
}
