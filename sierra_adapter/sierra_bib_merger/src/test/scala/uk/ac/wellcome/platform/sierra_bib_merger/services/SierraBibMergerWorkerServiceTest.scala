package uk.ac.wellcome.platform.sierra_bib_merger.services

import com.twitter.finatra.http.EmbeddedHttpServer
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.aws.SQSMessage
import uk.ac.wellcome.platform.sierra_bib_merger.Server
import uk.ac.wellcome.platform.sierra_bib_merger.locals.DynamoDBLocal
import uk.ac.wellcome.test.utils.SQSLocal
import uk.ac.wellcome.utils.JsonUtil


class SierraBibMergerWorkerServiceTest
  extends FunSpec
    with Matchers
    with SQSLocal
    with DynamoDBLocal {

  val bibMergerQueue: String = createQueueAndReturnUrl("test_bib_merger")

  def defineServer: EmbeddedHttpServer = {
    new EmbeddedHttpServer(
      new Server(),
      flags = Map(
        "aws.region" -> "localhost",
        "aws.sqs.queue.url" -> bibMergerQueue
      )
    )
  }

  def generateMessage(): SQSMessage = {
    SQSMessage(
      Some("subject"),
      "message",
      "topic",
      "messageType",
      "timestamp"
    )
  }

  private val server = defineServer

  it("should fail") {
    val message = JsonUtil
      .toJson(generateMessage())
      .get

    sqsClient.sendMessage(bibMergerQueue, message)

    server.start()

    eventually {
      false shouldBe true
    }

    server.close()

  }

}