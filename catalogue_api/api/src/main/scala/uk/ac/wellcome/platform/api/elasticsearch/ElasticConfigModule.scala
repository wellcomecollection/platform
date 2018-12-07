package uk.ac.wellcome.platform.api.elasticsearch

import com.google.inject.{Provides, Singleton}
import com.sksamuel.elastic4s.Index
import com.twitter.inject.TwitterModule
import uk.ac.wellcome.elasticsearch.DisplayElasticConfig

object ElasticConfigModule extends TwitterModule {
  private val indexV1name = flag[String]("es.index.v1", "V1 ES index name")
  private val indexV2name = flag[String]("es.index.v2", "V2 ES index name")

  @Singleton
  @Provides
  def providesElasticConfig(): DisplayElasticConfig =
    DisplayElasticConfig(
      indexV1 = Index(indexV1name()),
      indexV2 = Index(indexV2name())
    )
}
