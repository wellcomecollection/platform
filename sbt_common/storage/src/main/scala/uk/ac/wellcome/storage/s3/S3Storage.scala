package uk.ac.wellcome.storage.s3

import java.io.InputStream

import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.model.ObjectMetadata
import com.twitter.inject.Logging
import uk.ac.wellcome.storage.type_classes.StorageStrategy

import grizzled.slf4j.Logging

import scala.concurrent.{ExecutionContext, Future}
import scala.collection.JavaConverters._

object S3Storage extends Logging {

  private def normalizePathFragment(prefix: String) =
    prefix
      .stripPrefix("/")
      .stripSuffix("/")

  private def generateMetadata(
    userMetadata: Map[String, String]): ObjectMetadata = {
    val objectMetadata = new ObjectMetadata()

    objectMetadata.setUserMetadata(userMetadata.asJava)

    objectMetadata
  }

  def put[T](s3Client: AmazonS3)(
    bucketName: String
  )(t: T,
    keyPrefix: String = "",
    keySuffix: String = "",
    userMetadata: Map[String, String] = Map())(
    implicit storageStrategy: StorageStrategy[T],
    ec: ExecutionContext
  ): Future[S3ObjectLocation] = {

    val metadata = generateMetadata(userMetadata)
    val storageStream = storageStrategy.get(t)

    val prefix = normalizePathFragment(keyPrefix)
    val suffix = normalizePathFragment(keySuffix)
    val storageKey = storageStream.storageKey.value

    val key = s"$prefix/${storageKey}$suffix"

    info(s"Attempt: PUT object to s3://$bucketName/$key")
    val putObject = Future {
      s3Client.putObject(bucketName, key, storageStream.inputStream, metadata)
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
