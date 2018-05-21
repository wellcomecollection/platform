package uk.ac.wellcome.elasticsearch

case class ElasticConfig(
  documentType: String,
  indexV1name: String,
  indexV2name: String
)
