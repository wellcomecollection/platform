package uk.ac.wellcome.storage.s3

import com.google.inject.Inject
import com.twitter.inject.Logging
import io.circe.{Decoder, Encoder}
import uk.ac.wellcome.utils.GlobalExecutionContext.context
import uk.ac.wellcome.utils.JsonUtil._

import scala.concurrent.Future

class S3TypedObjectStore[T] @Inject()(
  stringStore: S3StringStore
)(implicit encoder: Encoder[T], decoder: Decoder[T]) extends Logging with ObjectStore[T] {

  def put(in: T, keyPrefix: String): Future[S3ObjectLocation] =
    Future.fromTry(toJson(in)).flatMap { content =>
      stringStore.put(content = content, keyPrefix = keyPrefix)
    }

  def get(s3ObjectLocation: S3ObjectLocation): Future[T] =
    stringStore.get(s3ObjectLocation = s3ObjectLocation).flatMap { content =>
      Future.fromTry(fromJson[T](content))
    }
}
