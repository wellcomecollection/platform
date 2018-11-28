package uk.ac.wellcome.platform.api.models

case class ApiConfig(
  host: String,
  scheme: String,
  defaultPageSize: Int,
  pathPrefix: String,
  contextSuffix: String
)
