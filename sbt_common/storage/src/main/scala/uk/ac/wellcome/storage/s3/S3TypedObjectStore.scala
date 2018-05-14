package uk.ac.wellcome.storage.s3

import com.amazonaws.services.s3.AmazonS3
import com.google.inject.Inject
import com.twitter.inject.Logging
import io.circe.{Decoder, Encoder}
import uk.ac.wellcome.utils.GlobalExecutionContext.context
import uk.ac.wellcome.utils.JsonUtil._

import scala.concurrent.Future

class S3TypedObjectStore[T] @Inject()(
  s3Client: AmazonS3,
  s3Config: S3Config
) extends Logging {

  val stringStore = new S3StringStore(
    s3Client = s3Client,
    s3Config = s3Config
  )

  def put(sourcedObject: T, keyPrefix: String)(
    implicit encoder: Encoder[T]): Future[S3ObjectLocation] =
    Future.fromTry(toJson(sourcedObject)).flatMap { content =>
      stringStore.put(content = content, keyPrefix = keyPrefix)
    }

  def get(s3ObjectLocation: S3ObjectLocation)(
    implicit decoder: Decoder[T]): Future[T] =
    stringStore.get(s3ObjectLocation = s3ObjectLocation).flatMap { content =>
      Future.fromTry(fromJson[T](content))
    }
}
