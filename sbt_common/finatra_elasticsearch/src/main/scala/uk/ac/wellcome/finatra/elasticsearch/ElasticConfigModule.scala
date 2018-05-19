package uk.ac.wellcome.finatra.elasticsearch

import com.google.inject.{Provides, Singleton}
import com.twitter.inject.TwitterModule
import uk.ac.wellcome.elasticsearch.ElasticConfig

object ElasticConfigModule extends TwitterModule {
  private val documentType =
    flag[String]("es.type", "item", "document type in Elasticsearch")

  @Singleton
  @Provides
  def providesElasticConfig(): ElasticConfig =
    ElasticConfig(
      documentType = documentType()
    )
}
