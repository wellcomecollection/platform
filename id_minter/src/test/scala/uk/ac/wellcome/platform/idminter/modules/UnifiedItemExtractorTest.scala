package uk.ac.wellcome.platform.idminter.modules

import com.amazonaws.services.sqs.model.Message
import com.fasterxml.jackson.core.JsonParseException
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.aws.SQSMessage
import uk.ac.wellcome.models.{Identifier, UnifiedItem}
import uk.ac.wellcome.utils.JsonUtil

class UnifiedItemExtractorTest extends FunSpec with Matchers with ScalaFutures with IntegrationPatience {

  val extractor = new UnifiedItemExtractor()
  it("extracts a unified item from an sqs message"){
    val unifiedItem = UnifiedItem("id", List(Identifier("Miro", "MiroId", "1234")), Option("super-secret"))
    val sqsMessage = SQSMessage(Some("subject"),UnifiedItem.json(unifiedItem), "topic", "messageType", "timestamp")
    val message = new Message().withBody(JsonUtil.toJson(sqsMessage).get)

    val eventualUnifiedItem = extractor.toUnifiedItem(message)

    whenReady(eventualUnifiedItem) {unifiedItem =>
      unifiedItem should be (unifiedItem)
    }

  }

  it("should return a failed future if it fails parsing the message it receives"){
    val sqsMessage = SQSMessage(Some("subject"),"somestring", "topic", "messageType", "timestamp")
    val message = new Message().withBody(JsonUtil.toJson(sqsMessage).get)

    val eventualUnifiedItem = extractor.toUnifiedItem(message)

    whenReady(eventualUnifiedItem.failed){e =>
      e shouldBe a [JsonParseException]
    }
  }

}
