package uk.ac.wellcome.elasticsearch.finatra.modules

import com.google.inject.Provides
import com.sksamuel.elastic4s.http.HttpClient
import javax.inject.Singleton
import org.apache.http.HttpHost
import org.elasticsearch.client.RestClient

object ElasticClientModule extends TwitterModule {
  private val host = flag[String]("es.host", "localhost", "host name of ES")
  private val port = flag[Int]("es.port", 9200, "port no of ES")
  private val protocol =
    flag[String]("es.protocol", "http", "protocol for talking to ES")
  private val clusterName =
    flag[String]("es.name", "elasticsearch", "cluster name")
  private val username = flag[String]("es.username", "elastic", "ES username")
  private val password = flag[String]("es.password", "changeme", "ES username")

  @Singleton
  @Provides
  def provideElasticClient(): HttpClient = {
    info(s"Building clientUri for ${host()}:${port()}")

    val restClient = RestClient
      .builder(new HttpHost(host(), port(), protocol()))
      .setHttpClientConfigCallback(
        new ElasticCredentials(username(), password()))
      // Needed for the snapshot_generator.
      // TODO Make this a flag
      .setMaxRetryTimeoutMillis(2000)
      .build()

    HttpClient.fromRestClient(restClient)
  }
}
