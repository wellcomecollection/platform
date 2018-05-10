package uk.ac.wellcome.elasticsearch.finatra.modules

import org.elasticsearch.client.RestClientBuilder.HttpClientConfigCallback
import org.apache.http.auth.{AuthScope, UsernamePasswordCredentials}
import org.apache.http.impl.client.BasicCredentialsProvider
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder


class ElasticClientConfig(username: String, password: String)
    extends HttpClientConfigCallback {
  val credentials = new UsernamePasswordCredentials(username, password)
  val credentialsProvider = new BasicCredentialsProvider()
  credentialsProvider.setCredentials(AuthScope.ANY, credentials)

  override def customizeHttpClient(
    httpClientBuilder: HttpAsyncClientBuilder): HttpAsyncClientBuilder = {
    httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider)
  }
}
