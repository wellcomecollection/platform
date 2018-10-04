package uk.ac.wellcome.platform.archive.common.models

case class HttpServerConfig(
  host: String,
  port: Int,
  externalBaseUrl: String
)
