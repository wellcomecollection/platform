package uk.ac.wellcome.s3

import java.net.URI

import scala.util.Try


case class S3ObjectLocation(bucket: String, key: String) {
  def toURI: URI = {
    new URI(s"s3://$bucket/$key")
  }
}

object S3ObjectLocation {
  def create(uri: URI): Try[S3ObjectLocation] =  for {
      scheme <- Try(uri.getScheme) if (scheme == "s3")
      host <- Try(uri.getHost)
      key <- Try(uri.getPath)
    } yield S3ObjectLocation(host, key.substring(1))
}
