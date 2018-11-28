package uk.ac.wellcome.platform.idminter.config.models

case class RDSClientConfig(
  host: String,
  port: Int,
  username: String,
  password: String
)
