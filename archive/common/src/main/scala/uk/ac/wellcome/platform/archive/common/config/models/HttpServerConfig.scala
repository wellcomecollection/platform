package uk.ac.wellcome.platform.archive.common.config.models

import java.net.URL

case class HttpServerConfig(
  host: String,
  port: Int,
  externalBaseUrl: String,
  contextUrl: URL
)

case class HTTPServerConfig(
  host: String,
  port: Int,
  externalBaseURL: String
)
