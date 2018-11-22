package uk.ac.wellcome.platform.reindex.reindex_worker.services

import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.messaging.test.fixtures.SNS
import uk.ac.wellcome.messaging.test.fixtures.SNS.Topic
import uk.ac.wellcome.models.work.generators.IdentifiersGenerators
import uk.ac.wellcome.test.fixtures.TestWith

import scala.concurrent.ExecutionContext.Implicits.global

class BulkSNSSenderTest
    extends FunSpec
    with Matchers
    with MockitoSugar
    with ScalaFutures
    with IdentifiersGenerators
    with IntegrationPatience
    with SNS {

  val messages: List[String] = (1 to 3).map { _ =>
    randomAlphanumeric(15)
  }.toList

  it("sends messages for the provided IDs") {
    withLocalSnsTopic { topic =>
      withBulkSNSSender(topic) { bulkSNSSender =>
        val future = bulkSNSSender.sendToSNS(messages = messages)

        whenReady(future) { _ =>
          val actualRecords = listMessagesReceivedFromSNS(topic).map {
            _.message
          }.distinct

          actualRecords should contain theSameElementsAs messages
        }
      }
    }
  }

  private def withBulkSNSSender[R](topic: Topic)(
    testWith: TestWith[BulkSNSSender, R]): R =
    withSNSWriter(topic) { snsWriter =>
      val bulkSNSSender = new BulkSNSSender(snsWriter = snsWriter)
      testWith(bulkSNSSender)
    }
}
