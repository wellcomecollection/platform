package uk.ac.wellcome.platform.idminter.steps

import com.amazonaws.services.sqs.model.Message
import com.fasterxml.jackson.core.JsonParseException
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.aws.SQSMessage
import uk.ac.wellcome.models.{SourceIdentifier, UnifiedItem}
import uk.ac.wellcome.utils.JsonUtil

class UnifiedItemExtractorTest
    extends FunSpec
    with Matchers
    with ScalaFutures
    with IntegrationPatience {

  it("extracts the unified item included in the SQS message") {
    val unifiedItem =
      UnifiedItem(identifiers =
                    List(SourceIdentifier("Miro", "MiroId", "1234")),
                  accessStatus = Option("super-secret"))
    val sqsMessage = SQSMessage(Some("subject"),
                                UnifiedItem.json(unifiedItem),
                                "topic",
                                "messageType",
                                "timestamp")
    val message = new Message().withBody(JsonUtil.toJson(sqsMessage).get)

    val eventualUnifiedItem = UnifiedItemExtractor.toUnifiedItem(message)

    whenReady(eventualUnifiedItem) { extractedUnifiedItem =>
      extractedUnifiedItem should be(unifiedItem)
    }

  }

  it("should return a failed future if it fails parsing the message it receives") {
    val sqsMessage = SQSMessage(Some("subject"),
                                "not a json string",
                                "topic",
                                "messageType",
                                "timestamp")
    val message = new Message().withBody(JsonUtil.toJson(sqsMessage).get)

    val eventualUnifiedItem = UnifiedItemExtractor.toUnifiedItem(message)

    whenReady(eventualUnifiedItem.failed) { e =>
      e shouldBe a[JsonParseException]
    }
  }

}
