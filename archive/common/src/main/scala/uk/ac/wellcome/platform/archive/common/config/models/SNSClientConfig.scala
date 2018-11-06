package uk.ac.wellcome.platform.archive.common.config.models

case class SNSClientConfig(
  accessKey: Option[String],
  secretKey: Option[String],
  endpoint: Option[String],
  region: String
)
