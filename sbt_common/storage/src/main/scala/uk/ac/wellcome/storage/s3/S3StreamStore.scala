package uk.ac.wellcome.storage.s3

import java.io.InputStream

import com.amazonaws.services.s3.AmazonS3
import com.google.inject.Inject
import grizzled.slf4j.Logging

import scala.concurrent.{ExecutionContext, Future}

import uk.ac.wellcome.storage.type_classes.StorageStrategyGenerator._

class S3StreamStore @Inject()(
  s3Client: AmazonS3
)(implicit ec: ExecutionContext)
    extends Logging
    with S3ObjectStore[InputStream] {

  override def put(bucket: String)(
    input: InputStream,
    keyPrefix: String,
    keySuffix: String = ""
  ): Future[S3ObjectLocation] =
    S3Storage.put(s3Client)(bucket)(input, keyPrefix, keySuffix)

  override def get(s3ObjectLocation: S3ObjectLocation): Future[InputStream] =
    S3Storage.get(s3Client)(s3ObjectLocation)
}
