package uk.ac.wellcome.config.core.models

case class AWSClientConfig(
  accessKey: Option[String],
  secretKey: Option[String],
  endpoint: Option[String],
  maxConnections: Option[String],
  region: String
)
