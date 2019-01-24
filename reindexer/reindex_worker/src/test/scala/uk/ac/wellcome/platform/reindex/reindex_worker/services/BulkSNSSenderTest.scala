package uk.ac.wellcome.platform.reindex.reindex_worker.services

import com.amazonaws.SdkClientException
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.messaging.fixtures.SNS
import uk.ac.wellcome.messaging.fixtures.SNS.Topic
import uk.ac.wellcome.platform.reindex.reindex_worker.fixtures.BulkSNSSenderFixture

import scala.util.Random

class BulkSNSSenderTest
    extends FunSpec
    with Matchers
    with MockitoSugar
    with ScalaFutures
    with BulkSNSSenderFixture
    with IntegrationPatience
    with SNS {

  val messages: List[String] = (1 to 3).map { _ =>
    Random.alphanumeric take 15 mkString
  }.toList

  it("sends messages for the provided IDs") {
    withLocalSnsTopic { topic =>
      withBulkSNSSender { bulkSNSSender =>
        val future = bulkSNSSender.sendToSNS(
          messages = messages,
          snsConfig = createSNSConfigWith(topic)
        )

        whenReady(future) { _ =>
          val actualRecords = listMessagesReceivedFromSNS(topic).map {
            _.message
          }.distinct

          actualRecords should contain theSameElementsAs messages
        }
      }
    }
  }

  it("returns a failed Future[SdkClientException] if there's an SNS error") {
    val badTopic = Topic("no-such-topic")
    withBulkSNSSender { bulkSNSSender =>
      val future = bulkSNSSender.sendToSNS(
        messages = messages,
        snsConfig = createSNSConfigWith(badTopic)
      )

      whenReady(future.failed) {
        _ shouldBe a[SdkClientException]
      }
    }
  }
}
