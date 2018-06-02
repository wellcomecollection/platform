package uk.ac.wellcome.storage.s3

import java.io.InputStream

import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.model.ObjectMetadata
import com.google.inject.Inject
import grizzled.slf4j.Logging
import uk.ac.wellcome.storage.{ObjectLocation, StorageBackend}

import scala.concurrent.{ExecutionContext, Future}
import scala.collection.JavaConverters._

class S3StorageBackend @Inject()(s3Client: AmazonS3)(
  implicit ec: ExecutionContext)
    extends StorageBackend
    with Logging {

  private def generateMetadata(
    userMetadata: Map[String, String]
  ): ObjectMetadata = {
    val objectMetadata = new ObjectMetadata()
    objectMetadata.setUserMetadata(userMetadata.asJava)
    objectMetadata
  }

  def put(location: ObjectLocation,
          input: InputStream,
          metadata: Map[String, String]): Future[Unit] = {
    val bucketName = location.namespace
    val key = location.key

    val generatedMetadata = generateMetadata(metadata)

    info(s"Attempt: PUT object to s3://$bucketName/$key")
    val putObject = Future {
      s3Client.putObject(bucketName, key, input, generatedMetadata)
    }

    putObject.map { _ =>
      info(s"Success: PUT object to s3://$bucketName/$key")
      ObjectLocation(bucketName, key)
    }
  }

  def get(location: ObjectLocation): Future[InputStream] = {
    val bucketName = location.namespace
    val key = location.key

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
