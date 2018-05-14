package uk.ac.wellcome.storage.s3

import scala.concurrent.Future

abstract trait S3Store[T] {
  def put(t: T, keyPrefix: String): Future[S3ObjectLocation]

  def get(s3ObjectLocation: S3ObjectLocation): Future[T]
}
