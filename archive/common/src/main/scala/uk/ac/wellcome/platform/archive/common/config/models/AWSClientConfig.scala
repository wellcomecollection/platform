package uk.ac.wellcome.platform.archive.common.config.models

case class AWSClientConfig(
  accessKey: Option[String],
  secretKey: Option[String],
  endpoint: Option[String],
  region: String
)
