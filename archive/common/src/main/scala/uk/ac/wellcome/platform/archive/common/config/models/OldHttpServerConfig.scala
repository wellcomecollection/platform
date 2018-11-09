package uk.ac.wellcome.platform.archive.common.config.models

import java.net.URL

case class OldHttpServerConfig(
  host: String,
  port: Int,
  externalBaseUrl: String,
  contextUrl: URL
)