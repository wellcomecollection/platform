package uk.ac.wellcome.platform.ingestor

import com.sksamuel.elastic4s.http.ElasticDsl._
import com.twitter.finatra.http.EmbeddedHttpServer
import com.twitter.inject.server.FeatureTestMixin
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.utils.JsonUtil._
import uk.ac.wellcome.models.aws.SQSMessage
import uk.ac.wellcome.models.{
  IdentifiedWork,
  IdentifierSchemes,
  SourceIdentifier
}
import uk.ac.wellcome.platform.ingestor.test.utils.Ingestor
import uk.ac.wellcome.test.utils.JsonTestUtil
import uk.ac.wellcome.utils.GlobalExecutionContext.context
import uk.ac.wellcome.utils.JsonUtil

import scala.collection.JavaConversions._

class IngestorFeatureTest
    extends FunSpec
    with Ingestor
    with FeatureTestMixin
    with Matchers
    with JsonTestUtil
    with ScalaFutures {

  override val server: EmbeddedHttpServer = createServer

  // Setting 1 second timeout for tests, so that test don't have to wait too long to test message deletion
  sqsClient.setQueueAttributes(queueUrl, Map("VisibilityTimeout" -> "1"))

  it("reads an identified work from the queue and ingests it") {
    val sourceIdentifier =
      SourceIdentifier(IdentifierSchemes.miroImageNumber, "5678")

    val workString = JsonUtil
      .toJson(
        IdentifiedWork(title = Some("A type of a tame turtle"),
                       sourceIdentifier = sourceIdentifier,
                       version = 1,
                       identifiers = List(sourceIdentifier),
                       canonicalId = "1234")
      )
      .get

    sqsClient.sendMessage(
      queueUrl,
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

    eventually {
      val hitsFuture = elasticClient
        .execute(search(s"$indexName/$itemType").matchAllQuery())
        .map { _.hits.hits }
      whenReady(hitsFuture) { hits =>
        hits should have size 1

        assertJsonStringsAreEqual(
          hits.head.sourceAsString,
          workString
        )
      }
    }
  }

  it("deletes a message from the queue if it fails processing") {
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
      queueUrl,
      invalidMessage
    )

    // After a message is read, it stays invisible for 1 second and then it gets sent again.
    // So we wait for longer than the visibility timeout and then we assert that it has become
    // invisible again, which means that the ingestor picked it up again,
    // and so it wasn't deleted as part of the first run.
    // TODO Write this test using dead letter queues once https://github.com/adamw/elasticmq/issues/69 is closed
    Thread.sleep(2000)

    eventually {
      sqsClient
        .getQueueAttributes(
          queueUrl,
          List("ApproximateNumberOfMessagesNotVisible")
        )
        .getAttributes
        .get(
          "ApproximateNumberOfMessagesNotVisible"
        ) shouldBe "1"
    }
  }
}
