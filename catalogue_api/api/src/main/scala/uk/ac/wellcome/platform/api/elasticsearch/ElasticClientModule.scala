package uk.ac.wellcome.platform.api.elasticsearch

import com.google.inject.{Provides, Singleton}
import com.sksamuel.elastic4s.http.ElasticClient
import com.twitter.inject.TwitterModule
import uk.ac.wellcome.elasticsearch.ElasticClientBuilder

object ElasticClientModule extends TwitterModule {
  private val hostname =
    flag[String]("es.host", "localhost", "host name of ES")
  private val hostPort = flag[Int]("es.port", 9200, "port no of ES")
  private val hostProtocol =
    flag[String]("es.protocol", "http", "protocol for talking to ES")
  private val username = flag[String]("es.username", "elastic", "ES username")
  private val password = flag[String]("es.password", "changeme", "ES username")

  @Singleton
  @Provides
  def providesElasticClient(): ElasticClient =
    ElasticClientBuilder.create(
      hostname = hostname(),
      port = hostPort(),
      protocol = hostProtocol(),
      username = username(),
      password = password()
    )
}
