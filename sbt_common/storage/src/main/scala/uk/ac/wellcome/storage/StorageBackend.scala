package uk.ac.wellcome.storage

import java.io.InputStream

import scala.concurrent.Future

trait StorageBackend {
  def put(location: ObjectLocation,
          input: InputStream,
          metadata: Map[String, String]): Future[Unit]
  def get(location: ObjectLocation): Future[InputStream]
}
