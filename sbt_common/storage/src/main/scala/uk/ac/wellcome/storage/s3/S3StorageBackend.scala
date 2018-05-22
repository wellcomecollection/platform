package uk.ac.wellcome.storage.s3

import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.model.ObjectMetadata
import uk.ac.wellcome.storage.{KeyPrefix, KeySuffix, ObjectLocation, ObjectStore}
import uk.ac.wellcome.storage.type_classes.StorageStrategy

import grizzled.slf4j.Logging

import scala.concurrent.{ExecutionContext, Future}
import scala.collection.JavaConverters._

class S3StorageBackend[T](s3Client: AmazonS3)(implicit ec: ExecutionContext)
  extends ObjectStore[T]
    with Logging {

  private def normalizePathFragment(prefix: String) = prefix
    .stripPrefix("/")
    .stripSuffix("/")

  private def generateMetadata(
                                userMetadata: Map[String, String]
                              ): ObjectMetadata = {
    val objectMetadata = new ObjectMetadata()
    objectMetadata.setUserMetadata(userMetadata.asJava)
    objectMetadata
  }

  def put(bucketName: String)(
    t: T,
    keyPrefix: KeyPrefix = KeyPrefix(""),
    keySuffix: KeySuffix = KeySuffix(""),
    userMetadata: Map[String, String] = Map()
  )(implicit storageStrategy: StorageStrategy[T]): Future[ObjectLocation] = {

    val metadata = generateMetadata(userMetadata)
    val storageStream = storageStrategy.store(t)

    val prefix = normalizePathFragment(keyPrefix.value)
    val suffix = normalizePathFragment(keySuffix.value)
    val storageKey = storageStream.storageKey.value

    val key = s"$prefix/${storageKey}$suffix"

    info(s"Attempt: PUT object to s3://$bucketName/$key")
    val putObject = Future {
      s3Client.putObject(bucketName, key, storageStream.inputStream, metadata)
    }

    putObject.map { _ =>
      info(s"Success: PUT object to s3://$bucketName/$key")
      ObjectLocation(bucketName, key)
    }
  }

  def get(objectLocation: ObjectLocation)(implicit storageStrategy: StorageStrategy[T]): Future[T] = {
    val bucketName = objectLocation.namespace
    val key = objectLocation.key

    info(s"Attempt: GET object from s3://$bucketName/$key")

    val futureInputStream = Future {
      s3Client.getObject(bucketName, key).getObjectContent
    }

    futureInputStream.foreach {
      case _ =>
        info(s"Success: GET object from s3://$bucketName/$key")
    }

    for {
      inputStream <- futureInputStream
      t <- Future.fromTry(storageStrategy.retrieve(inputStream))
    } yield t
  }
}