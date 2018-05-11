package uk.ac.wellcome.platform.snapshot_generator.finatra

import com.google.inject.{Provides, Singleton}
import com.sksamuel.elastic4s.http.HttpClient
import com.twitter.inject.TwitterModule
import uk.ac.wellcome.elasticsearch.{ElasticClientBuilder, ElasticConfig}

object SnapshotGeneratorModule extends TwitterModule {
  private val hostname = flag[String]("es.host", "localhost", "host name of ES")
  private val hostPort = flag[Int]("es.port", 9200, "port no of ES")
  private val hostProtocol =
    flag[String]("es.protocol", "http", "protocol for talking to ES")
  private val username = flag[String]("es.username", "elastic", "ES username")
  private val password = flag[String]("es.password", "changeme", "ES username")

  @Singleton
  @Provides
  def providesElasticConfig(): ElasticConfig =
    ElasticConfig(
      hostname = hostname(),
      hostPort = hostPort(),
      hostProtocol = hostProtocol(),
      username = username(),
      password = password()
    )

  @Singleton
  @Provides
  def providesElasticClient(elasticConfig: ElasticConfig): HttpClient =
    ElasticClientBuilder.buildElasticClient(elasticConfig)
}
