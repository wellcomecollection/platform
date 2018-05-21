package uk.ac.wellcome.storage.s3

import java.io.InputStream

import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.model.ObjectMetadata
import com.twitter.inject.Logging
import uk.ac.wellcome.storage.type_classes.{KeyGenerationStrategy, StreamGenerationStrategy}

import scala.concurrent.{ExecutionContext, Future}
import scala.collection.JavaConverters._


object S3Storage extends Logging {

  private def normalizePrefix(prefix: String) = prefix
    .stripPrefix("/")
    .stripSuffix("/")

  private def generateMetadata(userMetadata: Map[String, String]): ObjectMetadata = {
    val objectMetadata = new ObjectMetadata()

    objectMetadata.setUserMetadata(userMetadata.asJava)

    objectMetadata
  }

  def put[T](
    s3Client: AmazonS3)(
    bucketName: String
  )(t: T, keyPrefix: String = "", userMetadata: Map[String, String] = Map.empty[String, String])(
    implicit
      keyGenerationStrategy: KeyGenerationStrategy[T],
      streamStrategy: StreamGenerationStrategy[T],
      ec: ExecutionContext
  ): Future[S3ObjectLocation] = {

    val metadata = generateMetadata(userMetadata)
    val normalizedPrefix = normalizePrefix(keyPrefix)
    val generatedKey = keyGenerationStrategy.getKey(t)

    val key = s"$normalizedPrefix/$generatedKey"

    val input = streamStrategy.getStream(t)

    info(s"Attempt: PUT object to s3://$bucketName/$key")
    val putObject = Future {
      s3Client.putObject(bucketName, key, input, metadata)
    }

    putObject.map { _ =>
      info(s"Success: PUT object to s3://$bucketName/$key")
      S3ObjectLocation(bucketName, key)
    }
  }

  def get(s3Client: AmazonS3)(s3ObjectLocation: S3ObjectLocation)(
    implicit ec: ExecutionContext): Future[InputStream] = {

    val bucketName = s3ObjectLocation.bucket
    val key = s3ObjectLocation.key

    info(s"Attempt: GET object from s3://$bucketName/$key")

    val futureInputStream = Future {
      s3Client.getObject(bucketName, key).getObjectContent
    }

    futureInputStream.foreach {
      case _ =>
        info(s"Success: GET object from s3://$bucketName/$key")
    }

    futureInputStream
  }
}
