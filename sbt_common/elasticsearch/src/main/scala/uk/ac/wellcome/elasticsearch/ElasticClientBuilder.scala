package uk.ac.wellcome.elasticsearch

import com.sksamuel.elastic4s.http.HttpClient
import org.apache.http.HttpHost
import org.elasticsearch.client.RestClient

object ElasticClientBuilder {
  def buildClient(config: ElasticClientConfig) = {
    RestClient
      .builder(new HttpHost(config.hostname, config.hostPort, config.hostProtocol))
      .setHttpClientConfigCallback(
        new ElasticClientCredentials(config.elasticUsername, config.elasticPassword))
          // Needed for the snapshot_generator. // TODO Make this a flag
          .setMaxRetryTimeoutMillis(2000)
          .build()
  }

  def buildHttpClient(config: ElasticClientConfig) = {
    HttpClient.fromRestClient(buildClient(config))
  }
}
