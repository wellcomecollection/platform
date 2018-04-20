package uk.ac.wellcome.s3

import java.net.URI

object S3Uri {

  def apply(bucket: String, key: String): URI = new URI(s"s3://$bucket/$key")

  def unapply(uri: URI): Option[(String, String)] =
    for {
      scheme <- Option(uri.getScheme) if (scheme == "s3")
      host <- Option(uri.getHost)
      key <- Option(uri.getPath)
    } yield (host, key.substring(1))

}
