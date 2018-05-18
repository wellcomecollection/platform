package uk.ac.wellcome.storage.s3

import java.io.InputStream

import com.amazonaws.services.s3.AmazonS3
import com.google.inject.Inject
import com.twitter.inject.Logging

import scala.concurrent.{ExecutionContext, Future}

class S3StreamStore @Inject()(
  s3Client: AmazonS3
)(implicit ec: ExecutionContext)
    extends Logging
    with S3ObjectStore[InputStream] {

  override def put(bucket: String)(
    in: InputStream,
    keyPrefix: String): Future[S3ObjectLocation] =
    S3Storage.put(s3Client, bucket)(keyPrefix)(in)
  override def get(s3ObjectLocation: S3ObjectLocation): Future[InputStream] =
    S3Storage.get(s3Client, s3ObjectLocation.bucket)(s3ObjectLocation.key)
}
