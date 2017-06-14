package uk.ac.wellcome.test.utils

import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.ElasticsearchClientUri
import com.sksamuel.elastic4s.http.HttpClient
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsResponse
import org.elasticsearch.common.settings.Settings
import org.scalatest.concurrent.{Eventually, IntegrationPatience}
import org.scalatest.{Matchers, Suite}
import uk.ac.wellcome.utils.GlobalExecutionContext.context

import scala.concurrent.Future

trait ElasticSearchLocal
    extends Eventually
    with ExtendedPatience
    with Matchers { this: Suite =>

  private val settings = Settings
    .builder()
    .put("cluster.name", "wellcome")
    .put("xpack.security.user", "elastic:changeme")
    .build()

  val elasticClient = HttpClient(ElasticsearchClientUri("localhost", 9300))

  // Elasticsearch takes a while to start up so check that it actually started before running tests
  eventually {
    elasticClient.execute(clusterHealth()).await.getNumberOfNodes shouldBe 1
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

  private def deleteIndexIfExists(indexName: String, indexExistResponse: IndicesExistsResponse) = {
    if (indexExistResponse.isExists) elasticClient.execute(deleteIndex(indexName))
    else Future.successful(())
  }
}
