package uk.ac.wellcome.platform.ingestor

import com.sksamuel.elastic4s.http.ElasticDsl._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.utils.JsonUtil._
import uk.ac.wellcome.models.aws.SQSMessage
import uk.ac.wellcome.models.{
  IdentifiedWork,
  IdentifierSchemes,
  SourceIdentifier
}
import uk.ac.wellcome.elasticsearch.test.fixtures.ElasticsearchFixtures
import uk.ac.wellcome.test.fixtures.SQS
import uk.ac.wellcome.test.utils.JsonTestUtil
import uk.ac.wellcome.utils.GlobalExecutionContext.context
import uk.ac.wellcome.utils.JsonUtil

import scala.collection.JavaConversions._

class IngestorFeatureTest
    extends FunSpec
    with Matchers
    with JsonTestUtil
    with ScalaFutures
    with fixtures.Server
    with ElasticsearchFixtures
    with SQS {

  val indexName = "works"
  val itemType = "work"

  it("reads an identified work from the queue and ingests it") {
    val sourceIdentifier =
      SourceIdentifier(IdentifierSchemes.miroImageNumber, "Item", "5678")

    val workString = JsonUtil
      .toJson(
        IdentifiedWork(
          title = Some("A type of a tame turtle"),
          sourceIdentifier = sourceIdentifier,
          version = 1,
          identifiers = List(sourceIdentifier),
          canonicalId = "1234")
      )
      .get

    withLocalSqsQueue { queue =>
      sqsClient.sendMessage(
        queue.url,
        JsonUtil
          .toJson(
            SQSMessage(
              Some("identified-item"),
              workString,
              "ingester",
              "messageType",
              "timestamp"
            )
          )
          .get
      )

      val flags = sqsLocalFlags(queue) ++ esLocalFlags(indexName, itemType)

      withLocalElasticsearchIndex(indexName, itemType) { _ =>
        withServer(flags) { _ =>
          eventually {
            val hitsFuture = elasticClient
              .execute(search(s"$indexName/$itemType").matchAllQuery())
              .map {
                _.hits.hits
              }
            whenReady(hitsFuture) { hits =>
              hits should have size 1

              assertJsonStringsAreEqual(
                hits.head.sourceAsString,
                workString
              )
            }
          }
        }
      }
    }
  }

  it("does not delete a message from the queue if it fails processing") {
    withLocalSqsQueue { queue =>
      val invalidMessage = JsonUtil
        .toJson(
          SQSMessage(
            Some("identified-item"),
            "not a json string - this will fail parsing",
            "ingester",
            "messageType",
            "timestamp"
          )
        )
        .get

      sqsClient.sendMessage(
        queue.url,
        invalidMessage
      )

      val flags = sqsLocalFlags(queue) ++ esLocalFlags(indexName, itemType)

      withServer(flags) { _ =>
        // After a message is read, it stays invisible for 1 second and then it gets sent again.
        // So we wait for longer than the visibility timeout and then we assert that it has become
        // invisible again, which means that the ingestor picked it up again,
        // and so it wasn't deleted as part of the first run.
        // TODO Write this test using dead letter queues once https://github.com/adamw/elasticmq/issues/69 is closed
        Thread.sleep(2000)

        eventually {
          sqsClient
            .getQueueAttributes(
              queue.url,
              List("ApproximateNumberOfMessagesNotVisible")
            )
            .getAttributes
            .get(
              "ApproximateNumberOfMessagesNotVisible"
            ) shouldBe "1"
        }
      }
    }
  }
}
