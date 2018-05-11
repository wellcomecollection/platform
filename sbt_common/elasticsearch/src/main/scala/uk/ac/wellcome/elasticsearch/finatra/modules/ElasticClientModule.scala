package uk.ac.wellcome.elasticsearch.finatra.modules

import javax.inject.Singleton
import com.google.inject.Provides
import org.elasticsearch.client.RestClient
import org.elasticsearch.client.RestClientBuilder.HttpClientConfigCallback
import com.sksamuel.elastic4s.http.HttpClient
import com.twitter.inject.TwitterModule
import org.apache.http.auth.{AuthScope, UsernamePasswordCredentials}
import org.apache.http.HttpHost
import org.apache.http.impl.client.BasicCredentialsProvider
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder

class ElasticCredentials(username: String, password: String)
    extends HttpClientConfigCallback {
  val credentials = new UsernamePasswordCredentials(username, password)
  val credentialsProvider = new BasicCredentialsProvider()
  credentialsProvider.setCredentials(AuthScope.ANY, credentials)

  override def customizeHttpClient(
    httpClientBuilder: HttpAsyncClientBuilder): HttpAsyncClientBuilder = {
    httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider)
  }
}

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
    buildElasticClient(
      host = host(),
      port = port(),
      protocol = protocol(),
      username = username(),
      password = password()
    )
  }

  def buildElasticClient(host: String, port: Int, protocol: String, username: String, password: String): HttpClient = {
    val restClient = RestClient
      .builder(new HttpHost(host, port, protocol))
      .setHttpClientConfigCallback(
        new ElasticCredentials(username, password))
      // Needed for the snapshot_generator.
      // TODO Make this a flag
      .setMaxRetryTimeoutMillis(2000)
      .build()

    HttpClient.fromRestClient(restClient)
  }
}
