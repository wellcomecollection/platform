package uk.ac.wellcome.finatra.services

import javax.inject.{Inject, Singleton}
import com.sksamuel.elastic4s.ElasticClient

import uk.ac.wellcome.finatra.modules.ElasticClientModule

@Singleton
class ElasticsearchService @Inject()(
  elasticClient: ElasticClient
) {
  val client = elasticClient
}
