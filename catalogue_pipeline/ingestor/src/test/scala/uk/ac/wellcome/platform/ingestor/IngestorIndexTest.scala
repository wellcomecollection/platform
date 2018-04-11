package uk.ac.wellcome.platform.ingestor

import com.sksamuel.elastic4s.ElasticDsl.{deleteIndex, index}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FunSpec, Matchers}
import com.sksamuel.elastic4s.http.ElasticDsl._
import uk.ac.wellcome.elasticsearch.test.fixtures.ElasticsearchFixtures
import uk.ac.wellcome.test.fixtures.SQS

class IngestorIndexTest
    extends FunSpec
    with fixtures.Server
    with SQS
    with Matchers
    with ScalaFutures
    with ElasticsearchFixtures {

  val indexName = "works"
  val itemType = "work"

  it("creates the index at startup if it doesn't already exist") {
    elasticClient.execute(deleteIndex(indexName))

    eventually {
      val future = elasticClient.execute(index exists indexName)
      whenReady(future) { result =>
        result.isExists should be(false)
      }
    }

    withLocalSqsQueue { queue =>
      val flags = sqsLocalFlags(queue) ++ esLocalFlags(indexName, itemType)

      withServer(flags) { _ =>
        eventually {
          val future = elasticClient.execute(index exists indexName)
          whenReady(future) { result =>
            result.isExists should be(true)
          }
        }
      }
    }
  }
}
