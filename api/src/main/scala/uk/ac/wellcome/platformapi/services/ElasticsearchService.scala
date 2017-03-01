package uk.ac.wellcome.platform.api.services

import javax.inject.{Inject, Singleton}
import uk.ac.wellcome.finatra.modules.ElasticClientModule
import com.sksamuel.elastic4s.ElasticClient


@Singleton
class ElasticsearchService @Inject()(
  elasticClient: ElasticClient
) {
  val client = elasticClient
}
