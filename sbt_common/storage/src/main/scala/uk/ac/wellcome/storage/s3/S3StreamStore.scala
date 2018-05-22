package uk.ac.wellcome.storage.s3

import java.io.InputStream

import com.google.inject.Inject
import grizzled.slf4j.Logging
import uk.ac.wellcome.storage.{KeyPrefix, KeySuffix, ObjectLocation, ObjectStore}
import uk.ac.wellcome.storage.type_classes.StorageStrategy

import uk.ac.wellcome.storage.type_classes.StorageStrategyGenerator._

import scala.concurrent.{ExecutionContext, Future}

import scala.io.Source

class S3StreamStore @Inject()(
  storageBackend: S3StorageBackend[InputStream]
)(implicit ec: ExecutionContext)
    extends Logging
    with ObjectStore[InputStream] {

  def put(bucket: String)(
    input: InputStream,
    keyPrefix: KeyPrefix = KeyPrefix(""),
    keySuffix: KeySuffix = KeySuffix(""),
    userMetadata: Map[String, String] = Map()
  )(implicit storageStrategy: StorageStrategy[InputStream]): Future[ObjectLocation] =
    storageBackend.put(bucket)(input, keyPrefix, keySuffix, userMetadata)

  def get(objectLocation: ObjectLocation)(
    implicit storageStrategy: StorageStrategy[InputStream]
  ): Future[String] =
    storageBackend
      .get(objectLocation)
      .map(input => Source.fromInputStream(input).mkString)
}
