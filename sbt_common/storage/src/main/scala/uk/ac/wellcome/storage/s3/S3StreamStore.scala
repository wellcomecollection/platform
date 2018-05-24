package uk.ac.wellcome.storage.s3

import java.io.InputStream

import com.google.inject.Inject
import grizzled.slf4j.Logging
import uk.ac.wellcome.storage.{
  KeyPrefix,
  KeySuffix,
  ObjectLocation,
  ObjectStore
}

import uk.ac.wellcome.storage.type_classes.StorageStrategy

import scala.concurrent.Future

class S3StreamStore @Inject()(
  storageBackend: S3StorageBackend[InputStream]
) extends Logging
    with ObjectStore[InputStream] {

  def put(bucket: String)(
    input: InputStream,
    keyPrefix: KeyPrefix = KeyPrefix(""),
    keySuffix: KeySuffix = KeySuffix(""),
    userMetadata: Map[String, String] = Map()
  )(implicit storageStrategy: StorageStrategy[InputStream])
    : Future[ObjectLocation] =
    storageBackend.put(bucket)(input, keyPrefix, keySuffix, userMetadata)

  def get(objectLocation: ObjectLocation)(
    implicit storageStrategy: StorageStrategy[InputStream]
  ): Future[InputStream] =
    storageBackend
      .get(objectLocation)
}
