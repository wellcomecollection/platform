package uk.ac.wellcome.platform.sierra_reader.models

case class SierraConfig(
  resourceType: SierraResourceTypes.Value,
  apiUrl: String,
  oauthKey: String,
  oauthSec: String,
  fields: String
)
