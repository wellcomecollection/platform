package uk.ac.wellcome.platform.ingestor

import com.sksamuel.elastic4s.ElasticDsl._
import com.twitter.finatra.http.EmbeddedHttpServer
import com.twitter.inject.server.FeatureTestMixin
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.aws.SQSMessage
import uk.ac.wellcome.models.{IdentifiedWork, SourceIdentifier, Work}
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

  it("should read an identified unified item from the SQS queue and ingest it into Elasticsearch") {
    val identifiedWork = JsonUtil
      .toJson(
        IdentifiedWork(
          canonicalId = "1234",
          work = Work(
            identifiers = List(SourceIdentifier("Miro", "MiroID", "5678")), label = "some label")))
      .get

    sqsClient.sendMessage(
      ingestorQueueInfo.queueUrl,
      JsonUtil
        .toJson(
          SQSMessage(Some("identified-item"),
                     identifiedWork,
                     "ingester",
                     "messageType",
                     "timestamp"))
        .get
    )

    eventually {
      val hitsFuture = elasticClient.execute(search(s"$indexName/$itemType").matchAllQuery()).map(_.hits)
      whenReady(hitsFuture) { hits =>
        hits should have size 1
        hits.head.sourceAsString shouldBe identifiedWork
      }
    }
  }

  it("should not delete a message from the sqs queue if it fails processing it") {
    val invalidMessage = JsonUtil
      .toJson(
        SQSMessage(Some("identified-item"),
          "not a json string - this will fail parsing",
          "ingester",
          "messageType",
          "timestamp"))
      .get
    sqsClient.sendMessage(
      ingestorQueueInfo.queueUrl,
      invalidMessage
    )

    eventually {
      val messages = sqsClient.receiveMessage(ingestorQueueInfo.deadLetterQueueUrl).getMessages
      messages should have size (1)
      messages.head.getBody should be (invalidMessage)
    }
  }
}
