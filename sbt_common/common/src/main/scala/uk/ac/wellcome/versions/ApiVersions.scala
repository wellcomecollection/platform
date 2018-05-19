package uk.ac.wellcome.versions

import io.circe.{Decoder, Encoder}

object ApiVersions extends Enumeration {
  // We need to explicitly state the string representation of
  // each value as we've seen inconsistent behaviour where v2 would be
  // serialised as "v2" or "default" apparently unpredictably
  val v1 = Value("v1")
  val v2 = Value("v2")
  val default = v2

  implicit val decoder = Decoder.enumDecoder(ApiVersions)
  implicit val encoder = Encoder.enumEncoder(ApiVersions)
}
