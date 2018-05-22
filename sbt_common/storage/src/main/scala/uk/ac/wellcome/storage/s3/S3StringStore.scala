package uk.ac.wellcome.storage.s3

import com.google.inject.Inject
import grizzled.slf4j.Logging

import uk.ac.wellcome.storage.type_classes.StorageStrategy
import uk.ac.wellcome.storage.{KeyPrefix, KeySuffix, ObjectLocation, ObjectStore}

import scala.concurrent.Future

import uk.ac.wellcome.storage.type_classes.StorageStrategyGenerator._


class S3StringStore @Inject()(
  storageBackend: S3StorageBackend[String]
) extends Logging
    with ObjectStore[String]{

  override def put(bucket: String)(
    content: String,
    keyPrefix: KeyPrefix,
    keySuffix: KeySuffix = KeySuffix(""),
    userMetadata: Map[String, String] = Map()
  )(
    implicit storageStrategy: StorageStrategy[String]
  ): Future[ObjectLocation] = {
    storageBackend.put(bucket)(content, keyPrefix, keySuffix)
  }

  override def get(s3ObjectLocation: ObjectLocation)(
    implicit storageStrategy: StorageStrategy[String]
  ): Future[String] = {
    storageBackend.get(s3ObjectLocation)
  }
}