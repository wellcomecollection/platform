package uk.ac.wellcome.platform.archive.common.config.models

case class HTTPServerConfig(
  host: String,
  port: Int,
  externalBaseURL: String
)
