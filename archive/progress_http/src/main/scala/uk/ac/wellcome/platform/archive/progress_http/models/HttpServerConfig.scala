package uk.ac.wellcome.platform.archive.progress_http.models

case class HttpServerConfig(
  host: String,
  port: Int,
  externalBaseUrl: String
)
