package uk.ac.wellcome.storage.s3

import com.google.inject.Inject
import grizzled.slf4j.Logging

import io.circe.{Decoder, Encoder, Json}
import uk.ac.wellcome.storage.{KeyPrefix, KeySuffix, ObjectLocation, ObjectStore}
import uk.ac.wellcome.storage.type_classes.StorageStrategy
import io.circe.syntax._
import scala.concurrent.{ExecutionContext, Future}

import uk.ac.wellcome.storage.type_classes.StorageStrategyGenerator._


class S3TypeStore[T] @Inject()(
                                storageBackend: S3StorageBackend[Json]
)(implicit encoder: Encoder[T], decoder: Decoder[T], ec: ExecutionContext)
    extends Logging
    with ObjectStore[T] {

  def put(bucket: String)(
    input: T,
    keyPrefix: KeyPrefix = KeyPrefix(""),
    keySuffix: KeySuffix = KeySuffix(""),
    userMetadata: Map[String, String] = Map()
  )(
    implicit storageStrategy: StorageStrategy[T]
  ): Future[ObjectLocation] =
      storageBackend.put(bucket)(input.asJson, keyPrefix, keySuffix, userMetadata)

  def get(objectLocation: ObjectLocation)(
    implicit storageStrategy: StorageStrategy[T]
  ): Future[T] =
    storageBackend
      .get(objectLocation)
      .flatMap { content: Json =>
        Future.fromTry(decoder.decodeJson(content).toTry)
      }

}
