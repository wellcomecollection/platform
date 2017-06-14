package uk.ac.wellcome.test.utils

import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.ElasticsearchClientUri
import com.sksamuel.elastic4s.http.HttpClient
import com.sksamuel.elastic4s.http.index.admin.IndexExistsResponse
import org.apache.http.auth.{AuthScope, UsernamePasswordCredentials}
import org.apache.http.impl.client.BasicCredentialsProvider
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder
import org.elasticsearch.common.settings.Settings
import org.scalatest.concurrent.{Eventually, IntegrationPatience}
import org.scalatest.{Matchers, Suite}
import uk.ac.wellcome.utils.GlobalExecutionContext.context

import scala.concurrent.Future

import uk.ac.wellcome.finatra.modules.ElasticCredentials
import org.apache.http.auth.AuthScope
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder
import org.apache.http.auth.UsernamePasswordCredentials
import org.apache.http.impl.client.BasicCredentialsProvider
import org.apache.http.HttpHost
import org.elasticsearch.client.RestClient
import org.elasticsearch.client.RestClientBuilder.HttpClientConfigCallback

trait ElasticSearchLocal
    extends Eventually
    with ExtendedPatience
    with Matchers { this: Suite =>

  private val settings = Settings
    .builder()
    .put("cluster.name", "wellcome")
    .put("xpack.security.user", "elastic:changeme")
    .build()

  val restClient = RestClient
    .builder(new HttpHost("localhost", 9200, "http"))
    .setHttpClientConfigCallback(new ElasticCredentials())
    .build()

  val elasticClient = HttpClient.fromRestClient(restClient)

  // val elasticClient = HttpClient(ElasticsearchClientUri("localhost", 9200))

  // Elasticsearch takes a while to start up so check that it actually started before running tests
  eventually {
    elasticClient.execute(clusterHealth()).await.numberOfNodes shouldBe 1
  }

  def ensureIndexDeleted(indexName: String): Unit = {
    val future = for {
      indexExistQuery <- elasticClient.execute(indexExists(indexName))
      _ <- deleteIndexIfExists(indexName, indexExistQuery)
    } yield waitForIndexDeleted(indexName)
    future.await
  }

  private def waitForIndexDeleted(indexName: String) = {
    eventually {
      elasticClient
        .execute(indexExists(indexName)).await.isExists should be(false)
    }
  }

  private def deleteIndexIfExists(indexName: String, indexExistResponse: IndexExistsResponse) = {
    if (indexExistResponse.isExists) elasticClient.execute(deleteIndex(indexName))
    else Future.successful(())
  }
}
