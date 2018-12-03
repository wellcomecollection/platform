package uk.ac.wellcome.elasticsearch

import com.sksamuel.elastic4s.Index

case class DisplayElasticConfig(
  documentType: String,
  indexV1: Index,
  indexV2: Index
)
