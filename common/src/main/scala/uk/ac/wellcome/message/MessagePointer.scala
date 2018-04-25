package uk.ac.wellcome.message

import java.net.URI

import uk.ac.wellcome.s3.S3ObjectLocation

import scala.util.Try

case class MessagePointer(src: S3ObjectLocation)

object MessagePointer {
  def create(uri: URI): Try[MessagePointer] =
    S3ObjectLocation
      .create(uri)
      .map(MessagePointer(_))

}
