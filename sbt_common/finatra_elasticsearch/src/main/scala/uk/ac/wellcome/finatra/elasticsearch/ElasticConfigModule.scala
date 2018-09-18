package uk.ac.wellcome.finatra.elasticsearch

import com.google.inject.{Provides, Singleton}
import com.twitter.inject.TwitterModule
import uk.ac.wellcome.elasticsearch.DisplayElasticConfig

object ElasticConfigModule extends TwitterModule {
  private val documentType =
    flag[String]("es.type", "item", "document type in Elasticsearch")

  private val indexV1name = flag[String]("es.index.v1", "V1 ES index name")
  private val indexV2name = flag[String]("es.index.v2", "V2 ES index name")

  @Singleton
  @Provides
  def providesElasticConfig(): DisplayElasticConfig =
    DisplayElasticConfig(
      documentType = documentType(),
      indexV1name = indexV1name(),
      indexV2name = indexV2name()
    )
}
