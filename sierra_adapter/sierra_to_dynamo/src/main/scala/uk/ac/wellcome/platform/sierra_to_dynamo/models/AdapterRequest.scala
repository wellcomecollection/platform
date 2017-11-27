package uk.ac.wellcome.platform.sierra_to_dynamo.models

case class AdapterRequest(
  params: Map[String, String],
  startedBy: Option[String]
)
