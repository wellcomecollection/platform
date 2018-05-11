package uk.ac.wellcome.elasticsearch

import com.sksamuel.elastic4s.http.HttpClient
import org.apache.http.HttpHost
import org.apache.http.auth.{AuthScope, UsernamePasswordCredentials}
import org.apache.http.impl.client.BasicCredentialsProvider
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder
import org.elasticsearch.client.RestClient
import org.elasticsearch.client.RestClientBuilder.HttpClientConfigCallback

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

object ElasticClientBuilder {
  def buildElasticClient(elasticConfig: ElasticConfig): HttpClient = {
    val restClient = RestClient
      .builder(
        new HttpHost(
          elasticConfig.hostname,
          elasticConfig.hostPort,
          elasticConfig.hostProtocol))
      .setHttpClientConfigCallback(
        new ElasticCredentials(elasticConfig.username, elasticConfig.password))
      // Needed for the snapshot_generator.
      // TODO Make this a flag
      .setMaxRetryTimeoutMillis(2000)
      .build()

    HttpClient.fromRestClient(restClient)
  }
}
