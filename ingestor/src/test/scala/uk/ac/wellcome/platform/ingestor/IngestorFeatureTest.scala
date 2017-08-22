package uk.ac.wellcome.platform.ingestor

import com.sksamuel.elastic4s.http.ElasticDsl._
import com.twitter.finatra.http.EmbeddedHttpServer
import com.twitter.inject.server.FeatureTestMixin
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.finatra.modules.IdentifierSchemes
import uk.ac.wellcome.models.aws.SQSMessage
import uk.ac.wellcome.models.{SourceIdentifier, Work}
import uk.ac.wellcome.utils.GlobalExecutionContext.context
import uk.ac.wellcome.utils.JsonUtil

import scala.collection.JavaConversions._

class IngestorFeatureTest
    extends FunSpec
    with IngestorUtils
    with FeatureTestMixin
    with Matchers
    with ScalaFutures {

  override val server: EmbeddedHttpServer = createServer

  // Setting 1 second timeout for tests, so that test don't have to wait too long to test message deletion
  sqsClient.setQueueAttributes(ingestorQueueUrl,
                               Map("VisibilityTimeout" -> "1"))

  it(
    "should read an identified unified item from the SQS queue and ingest it into Elasticsearch") {
    val work = JsonUtil
      .toJson(
        Work(
          canonicalId = "1234",
          List(SourceIdentifier(IdentifierSchemes.miroImageNumber, "5678")),
          title = "A type of a tame turtle")
      ).get

    sqsClient.sendMessage(
      ingestorQueueUrl,
      JsonUtil
        .toJson(
          SQSMessage(Some("identified-item"),
                     work,
                     "ingester",
                     "messageType",
                     "timestamp"))
        .get
    )

    eventually {
      val hitsFuture = elasticClient
        .execute(search(s"$indexName/$itemType").matchAllQuery())
        .map { _.hits.hits }
      whenReady(hitsFuture) { hits =>
        hits should have size 1
        hits.head.sourceAsString shouldBe work
      }
    }
  }

  it(
    "should not delete a message from the sqs queue if it fails processing it") {
    val invalidMessage = JsonUtil
      .toJson(
        SQSMessage(Some("identified-item"),
                   "not a json string - this will fail parsing",
                   "ingester",
                   "messageType",
                   "timestamp"))
      .get
    sqsClient.sendMessage(
      ingestorQueueUrl,
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
        .getQueueAttributes(ingestorQueueUrl,
                            List("ApproximateNumberOfMessagesNotVisible"))
        .getAttributes
        .get("ApproximateNumberOfMessagesNotVisible") shouldBe "1"
    }
  }
}
