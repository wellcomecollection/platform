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

  it("extracts the work included in the SQS message") {

    val miroID = "M0000001"
    val label = "A note about a narwhal"

    val work = Work(
      identifiers = List(SourceIdentifier("Miro", "MiroId", miroID)),
      label = label
    )
    val sqsMessage = SQSMessage(Some("subject"),
                                JsonUtil.toJson(work).get,
                                "topic",
                                "messageType",
                                "timestamp")
    val message = new Message().withBody(JsonUtil.toJson(sqsMessage).get)

    val eventualWork = WorkExtractor.toWork(message)


    whenReady(eventualWork) { extractedWork =>
      extractedWork.identifiers.head.value shouldBe miroID
      extractedWork.label shouldBe label
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
