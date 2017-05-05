package uk.ac.wellcome.platform.idminter.steps

import com.amazonaws.services.sqs.model.Message
import com.fasterxml.jackson.core.JsonParseException
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.aws.SQSMessage
import uk.ac.wellcome.models.{SourceIdentifier, Work}
import uk.ac.wellcome.utils.JsonUtil

class WorkExtractorTest
    extends FunSpec
    with Matchers
    with ScalaFutures
    with IntegrationPatience {

  it("extracts the unified item included in the SQS message") {
    val work =
      Work(identifiers =
                    List(SourceIdentifier("Miro", "MiroId", "1234")),
                  label = "this is the item label",
                  accessStatus = Option("super-secret"))
    val sqsMessage = SQSMessage(Some("subject"),
                                JsonUtil.toJson(work).get,
                                "topic",
                                "messageType",
                                "timestamp")
    val message = new Message().withBody(JsonUtil.toJson(sqsMessage).get)

    val eventualWork = WorkExtractor.toWork(message)

    whenReady(eventualWork) { extractedWork =>
      extractedWork should be(work)
    }

  }

  it("should return a failed future if it fails parsing the message it receives") {
    val sqsMessage = SQSMessage(Some("subject"),
                                "not a json string",
                                "topic",
                                "messageType",
                                "timestamp")
    val message = new Message().withBody(JsonUtil.toJson(sqsMessage).get)

    val eventualWork = WorkExtractor.toWork(message)

    whenReady(eventualWork.failed) { e =>
      e shouldBe a[JsonParseException]
    }
  }

}
