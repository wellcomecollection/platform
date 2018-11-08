package uk.ac.wellcome.config.core.models

case class HTTPServerConfig(
  host: String,
  port: Int,
  externalBaseURL: String
)
