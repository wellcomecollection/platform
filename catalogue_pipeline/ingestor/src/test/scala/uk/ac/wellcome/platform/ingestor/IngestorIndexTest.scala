package uk.ac.wellcome.platform.ingestor

import com.sksamuel.elastic4s.ElasticDsl.{deleteIndex, index}
import com.sksamuel.elastic4s.http.ElasticDsl._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.elasticsearch.test.fixtures.ElasticsearchFixtures
import uk.ac.wellcome.messaging.test.fixtures.{Messaging, SQS}
import uk.ac.wellcome.storage.test.fixtures.S3

class IngestorIndexTest
    extends FunSpec
    with fixtures.Server
    with SQS
    with S3
    with Matchers
    with ScalaFutures
    with Messaging
    with ElasticsearchFixtures {

  val indexNameV1 = "works-v1"
  val indexNameV2 = "works-v2"
  val itemType = "work"

  it("creates the index at startup if it doesn't already exist") {
    deleteIndexIfExists(indexNameV1)
    deleteIndexIfExists(indexNameV2)

    withLocalSqsQueue { queue =>
      withLocalS3Bucket { bucket =>
        withServer(queue, bucket, indexNameV1, indexNameV2, itemType) { _ =>
          eventuallyIndexExists(indexNameV1)
          eventuallyIndexExists(indexNameV2)
        }
      }
    }
  }

  private def eventuallyIndexExists(indexName: String): Any = {
    eventually {
      val future = elasticClient.execute(index exists indexName)
      whenReady(future) { result =>
        result.isExists should be(true)
      }
    }
  }

  private def deleteIndexIfExists(indexName: String) = {
    elasticClient.execute(deleteIndex(indexName))

    eventually {
      val future = elasticClient.execute(index exists indexName)
      whenReady(future) { result =>
        result.isExists should be(false)
      }
    }
  }
}
