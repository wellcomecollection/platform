package uk.ac.wellcome.platform.ingestor

import com.sksamuel.elastic4s.ElasticDsl.{deleteIndex, index}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FunSpec, Matchers}
import com.sksamuel.elastic4s.http.ElasticDsl._
import uk.ac.wellcome.test.fixtures.{ServerFixtures, SqsFixtures}
import uk.ac.wellcome.test.utils.IndexedElasticSearchLocal

class IngestorIndexTest
    extends FunSpec
    with IndexedElasticSearchLocal
    with ServerFixtures[Server]
    with SqsFixtures
    with Matchers
    with ScalaFutures {

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

    withLocalSqsQueue { queueUrl =>
      val flags = Map(
        "aws.region" -> "localhost",
        "aws.sqs.queue.url" -> queueUrl,
        "aws.sqs.waitTime" -> "1"
      ) ++ sqsLocalFlags ++ esLocalFlags

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
