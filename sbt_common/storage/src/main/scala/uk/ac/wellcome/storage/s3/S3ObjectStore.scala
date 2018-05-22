package uk.ac.wellcome.storage.s3

import scala.concurrent.Future

trait S3ObjectStore[T] {
  def put(bucket: String)(in: T,
                          keyPrefix: String,
                          keySuffix: String = ""): Future[S3ObjectLocation]
  def get(s3ObjectLocation: S3ObjectLocation): Future[T]
}
