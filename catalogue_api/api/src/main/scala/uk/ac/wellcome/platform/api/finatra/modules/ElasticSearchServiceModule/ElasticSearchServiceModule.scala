package uk.ac.wellcome.platform.api.finatra.modules.ElasticSearchServiceModule

import com.google.inject.Provides
import com.sksamuel.elastic4s.http.HttpClient
import com.twitter.inject.TwitterModule
import uk.ac.wellcome.elasticsearch.finatra.modules.ElasticClientModule
import uk.ac.wellcome.platform.api.services.ElasticSearchService

object ElasticSearchServiceModule extends TwitterModule {
  override val modules = Seq(ElasticClientModule)

  private val defaultIndex = flag[String](
    name = "es.index",
    default = "records",
    help = "ES index name")
  private val documentType =
    flag[String](name = "es.type", default = "item", help = "ES document type")

  @Provides
  def providesElasticSearchService(
    elasticClient: HttpClient): ElasticSearchService =
    new ElasticSearchService(
      documentType = documentType(),
      elasticClient = elasticClient)

}
