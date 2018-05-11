package uk.ac.wellcome.platform.api.finatra.modules.ElasticSearchServiceModule

import com.google.inject.Provides
import com.sksamuel.elastic4s.http.HttpClient
import com.twitter.inject.TwitterModule
import uk.ac.wellcome.elasticsearch.{ElasticClientBuilder, ElasticClientConfig}
import uk.ac.wellcome.finatra.modules.ElasticClientConfigModule
import uk.ac.wellcome.platform.api.services.ElasticSearchService

object ElasticSearchServiceModule extends TwitterModule {
  override val modules = Seq(ElasticClientConfigModule)

  private val defaultIndex = flag[String](
    name = "es.index",
    default = "records",
    help = "ES index name")
  private val documentType =
    flag[String](name = "es.type", default = "item", help = "ES document type")

  @Provides
  def providesElasticSearchService(
    elasticClientConfig: ElasticClientConfig): ElasticSearchService =
    new ElasticSearchService(
      documentType = documentType(),
      elasticClientConfig = elasticClientConfig)
}
