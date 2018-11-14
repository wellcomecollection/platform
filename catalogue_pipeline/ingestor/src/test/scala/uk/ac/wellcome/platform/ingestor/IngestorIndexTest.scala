package uk.ac.wellcome.platform.ingestor

import com.sksamuel.elastic4s.ElasticDsl.{deleteIndex, index}
import com.sksamuel.elastic4s.http.ElasticDsl._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{Assertion, FunSpec, Matchers}
import uk.ac.wellcome.elasticsearch.test.fixtures.ElasticsearchFixtures
import uk.ac.wellcome.messaging.test.fixtures.{Messaging, SQS}

class IngestorIndexTest
    extends FunSpec
    with fixtures.Server
    with SQS
    with Matchers
    with ScalaFutures
    with Messaging
    with ElasticsearchFixtures {

  it("creates the index at startup if it doesn't already exist") {
    val indexName = "works"

    deleteIndexIfExists(indexName)

    withLocalSqsQueue { queue =>
      withServer(queue, indexName) { _ =>
        eventuallyIndexExists(indexName)
      }
    }
  }

  private def eventuallyIndexExists(indexName: String): Assertion =
    eventually {
      val future = elasticClient.execute(index exists indexName)
      whenReady(future) { result =>
        result.isExists should be(true)
      }
    }

  private def deleteIndexIfExists(indexName: String): Assertion = {
    elasticClient.execute(deleteIndex(indexName))

    eventually {
      val future = elasticClient.execute(index exists indexName)
      whenReady(future) { result =>
        result.isExists should be(false)
      }
    }
  }
}
