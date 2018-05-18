package uk.ac.wellcome.storage.s3

import java.io.ByteArrayInputStream

import com.amazonaws.services.s3.AmazonS3
import com.google.inject.Inject
import com.twitter.inject.Logging
import io.circe.{Decoder, Encoder}
import uk.ac.wellcome.storage.GlobalExecutionContext.context
import uk.ac.wellcome.utils.JsonUtil._

import scala.concurrent.Future
import scala.io.Source

class S3TypeStore[T] @Inject()(
  s3Client: AmazonS3
)(implicit encoder: Encoder[T], decoder: Decoder[T])
    extends Logging
    with S3ObjectStore[T] {

  def put(bucket: String)(in: T, keyPrefix: String): Future[S3ObjectLocation] =
    Future.fromTry(toJson(in)).flatMap { content =>
      val is = new ByteArrayInputStream(content.getBytes)

      S3Storage.put(s3Client, bucket)(keyPrefix)(is)
    }

  def get(s3ObjectLocation: S3ObjectLocation): Future[T] =
    S3Storage
      .get(s3Client, s3ObjectLocation.bucket)(s3ObjectLocation.key)
      .map(is => Source.fromInputStream(is).mkString)
      .flatMap { content =>
        Future.fromTry(fromJson[T](content))
      }
}
