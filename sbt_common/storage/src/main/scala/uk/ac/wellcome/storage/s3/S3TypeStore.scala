package uk.ac.wellcome.storage.s3

import com.amazonaws.services.s3.AmazonS3
import com.google.inject.Inject
import com.twitter.inject.Logging
import io.circe.{Decoder, Encoder}
import uk.ac.wellcome.utils.JsonUtil._

import scala.concurrent.{ExecutionContext, Future}
import scala.io.Source
import uk.ac.wellcome.storage.type_classes.KeyGenerator._
import uk.ac.wellcome.storage.type_classes.StreamGenerator._


class S3TypeStore[T] @Inject()(
  s3Client: AmazonS3
)(implicit encoder: Encoder[T], decoder: Decoder[T], ec: ExecutionContext)
    extends Logging
    with S3ObjectStore[T] {

  override def put(bucket: String)(input: T, keyPrefix: String): Future[S3ObjectLocation] = {
    Future.fromTry(toJson(input)).flatMap(jsonInput =>
      S3Storage.put(s3Client)(bucket)(jsonInput, keyPrefix)
    )
  }

  override def get(s3ObjectLocation: S3ObjectLocation): Future[T] = {
    S3Storage.get(s3Client)(s3ObjectLocation)
      .map(is => Source.fromInputStream(is).mkString)
      .flatMap { content =>
        Future.fromTry(fromJson[T](content))
      }
  }
}
