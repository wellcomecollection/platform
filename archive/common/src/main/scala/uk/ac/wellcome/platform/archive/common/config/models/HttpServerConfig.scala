package uk.ac.wellcome.platform.archive.common.config.models

case class HttpServerConfig(
  host: String,
  port: Int,
  externalBaseUrl: String
)
