package uk.ac.wellcome.elasticsearch.test.utils

import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.http.HttpClient
import com.sksamuel.elastic4s.http.index.admin.IndexExistsResponse
import org.apache.http.HttpHost
import org.elasticsearch.client.RestClient
import org.scalatest.concurrent.{Eventually, ScalaFutures}
import org.scalatest.{Assertion, Matchers, Suite}
import uk.ac.wellcome.elasticsearch.ElasticSearchIndex
import uk.ac.wellcome.finatra.modules.ElasticCredentials
import uk.ac.wellcome.utils.GlobalExecutionContext.context

import scala.concurrent.Future

trait ElasticSearchLocal
    extends Eventually
    with ExtendedPatience
    with ScalaFutures
    with Matchers { this: Suite =>

  val restClient: RestClient = RestClient
    .builder(new HttpHost("localhost", 9200, "http"))
    .setHttpClientConfigCallback(new ElasticCredentials("elastic", "changeme"))
    .build()

  val elasticClient: HttpClient = HttpClient.fromRestClient(restClient)

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

  def createAndWaitIndexIsCreated(index: ElasticSearchIndex,
                                  indexName: String): Assertion = {
    val createIndexFuture = index.create

    whenReady(createIndexFuture) { _ =>
      eventually {
        elasticClient
          .execute(indexExists(indexName))
          .await
          .isExists should be(true)
      }
    }
  }

  private def waitForIndexDeleted(indexName: String) = {
    eventually {
      elasticClient
        .execute(indexExists(indexName))
        .await
        .isExists should be(false)
    }
  }

  private def deleteIndexIfExists(indexName: String,
                                  indexExistResponse: IndexExistsResponse) = {
    if (indexExistResponse.isExists)
      elasticClient.execute(deleteIndex(indexName))
    else Future.successful(())
  }
}
