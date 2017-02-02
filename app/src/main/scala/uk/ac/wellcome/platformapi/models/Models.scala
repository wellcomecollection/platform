package uk.ac.wellcome.platform.api.models

case class Collection(
  refno: String,
  title: String
)

case class Response(
  refno: String,
  entries: Record,
  parent: Option[Collection]
)



