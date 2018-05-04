package uk.ac.wellcome.storage.s3

import com.amazonaws.services.s3.AmazonS3
import com.google.inject.Inject
import com.twitter.inject.Logging
import io.circe.Decoder
import uk.ac.wellcome.utils.JsonUtil

import scala.concurrent.Future
import scala.io.Source

class S3ObjectReader[T] @Inject()(
  s3Client: AmazonS3,
  s3Config: S3Config
) extends Logging {
  def get(s3ObjectLocation: S3ObjectLocation)(
    implicit decoder: Decoder[T]): Future[T] = {
    val bucket = s3ObjectLocation.bucket
    val key = s3ObjectLocation.key

    if (bucket != s3Config.bucketName) {
      debug(
        s"Bucket name in S3ObjectLocation ($bucket) does not match configured bucket (${s3Config.bucketName})")
    }

    S3ObjectReader.get[T](s3Client, bucket)(key)
  }
}

object S3ObjectReader extends Logging {
  def get[T](s3Client: AmazonS3, bucketName: String)(key: String)(
    implicit decoder: Decoder[T]): Future[T] = {

    info(s"Attempting to GET object from s3://$bucketName/$key")

    val getObject = Future {
      val s3Object = s3Client.getObject(bucketName, key)
      Source.fromInputStream(s3Object.getObjectContent).mkString
    }

    getObject.flatMap(s3ObjectContent => {
      info(s"Successful GET object from s3://$bucketName/$key")

      Future.fromTry(JsonUtil.fromJson[T](s3ObjectContent))
    })
  }
}
