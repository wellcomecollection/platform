package uk.ac.wellcome.elasticsearch

import com.sksamuel.elastic4s.Index

case class DisplayElasticConfig(
  indexV1: Index,
  indexV2: Index
) {
  def indexNameV1: String = indexV1.name
  def indexNameV2: String = indexV2.name
}
