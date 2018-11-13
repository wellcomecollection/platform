package uk.ac.wellcome.platform.ingestor.modules

import com.google.inject.{Provides, Singleton}
import com.twitter.inject.TwitterModule
import uk.ac.wellcome.platform.ingestor.config.models.IngestElasticConfig

object IngestElasticConfigModule extends TwitterModule {
  private val documentType =
    flag[String]("es.type", "item", "document type in Elasticsearch")

  private val index = flag[String]("es.index", "V1 ES index name")

  @Singleton
  @Provides
  def providesElasticConfig(): IngestElasticConfig =
    IngestElasticConfig(
      documentType = documentType(),
      indexName = index()
    )
}
