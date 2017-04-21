package uk.ac.wellcome.ingestor.services

import com.fasterxml.jackson.core.JsonParseException
import com.sksamuel.elastic4s.testkit.ElasticSugar
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.platform.ingestor.services.MessageProcessorService
import uk.ac.wellcome.utils.GlobalExecutionContext.context

class MessageProcessorServiceTest extends FunSpec with ElasticSugar with ScalaFutures with Matchers{

  it("should return a failed future if the input string is not a unified item"){
    ensureIndexExists("records")
    val messageProcessorService = new MessageProcessorService("records", "item", client)
    val future = messageProcessorService.indexUnifiedItem("a document")

    whenReady(future.failed){exception =>
      exception shouldBe a [JsonParseException]
    }

  }
}
