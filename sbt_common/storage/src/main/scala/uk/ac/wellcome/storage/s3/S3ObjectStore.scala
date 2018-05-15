package uk.ac.wellcome.storage.s3

import scala.concurrent.Future


trait S3ObjectStore[T] {
  def put(in: T, keyPrefix: String): Future[S3ObjectLocation]
  def get(s3ObjectLocation: S3ObjectLocation): Future[T]
}
