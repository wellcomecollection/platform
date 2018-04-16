package uk.ac.wellcome.s3

import java.net.URI

object S3Uri {

  def unapply(uri: URI): Option[(String, String)] =
    for {
      scheme <- Option(uri.getScheme) if (scheme == "s3")
      host <- Option(uri.getHost)
      key <- Option(uri.getPath)
    } yield (host, key.substring(1))

}
