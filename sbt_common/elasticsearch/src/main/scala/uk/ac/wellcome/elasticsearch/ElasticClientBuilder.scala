package uk.ac.wellcome.elasticsearch

import com.sksamuel.elastic4s.http.ElasticClient
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
  def create(hostname: String,
             port: Int,
             protocol: String,
             username: String,
             password: String): ElasticClient = {
    val restClient = RestClient
      .builder(new HttpHost(hostname, port, protocol))
      .setHttpClientConfigCallback(new ElasticCredentials(username, password))
      // Needed for the snapshot_generator.
      // TODO Make this a flag
      .setMaxRetryTimeoutMillis(2000)
      .build()

    ElasticClient.fromRestClient(restClient)
  }
}
