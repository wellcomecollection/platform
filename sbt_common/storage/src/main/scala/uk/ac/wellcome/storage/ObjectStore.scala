package uk.ac.wellcome.storage

import uk.ac.wellcome.storage.type_classes.StorageStrategy

import scala.concurrent.Future

case class KeyPrefix(value: String) extends AnyVal
case class KeySuffix(value: String) extends AnyVal

trait ObjectStore[T] {
  def put(namespace: String)(
    input: T,
    keyPrefix: KeyPrefix = KeyPrefix(""),
    keySuffix: KeySuffix = KeySuffix(""),
    userMetadata: Map[String, String] = Map()
  )(
           implicit storageStrategy: StorageStrategy[T]
         ): Future[ObjectLocation]

  def get(objectLocation: ObjectLocation)(
    implicit storageStrategy: StorageStrategy[T]
  ): Future[T]
}