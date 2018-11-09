package uk.ac.wellcome.platform.reindex.reindex_worker.services

import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.messaging.test.fixtures.SNS
import uk.ac.wellcome.messaging.test.fixtures.SNS.Topic
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.platform.reindex.reindex_worker.exceptions.ReindexerException
import uk.ac.wellcome.storage.ObjectLocation
import uk.ac.wellcome.storage.vhs.HybridRecord

import scala.concurrent.ExecutionContext.Implicits.global

class BulkSNSWriterTest
    extends FunSpec
    with Matchers
    with MockitoSugar
    with ScalaFutures
    with IntegrationPatience
    with SNS {

  val records: List[HybridRecord] = List("miro/1", "miro/2", "miro/3").map { id =>
    HybridRecord(
      id = id,
      version = 1,
      location = ObjectLocation(
        namespace = "s3://example-bukkit",
        key = "mykey.txt"
      )
    )
  }

  it("sends messages for the provided IDs") {
    withLocalSnsTopic { topic =>
      withSNSWriter(topic) { snsWriter =>
        val bulkSNSWriter = new BulkSNSWriter(snsWriter = snsWriter)

        val future = bulkSNSWriter.sendToSNS(records = records)

        whenReady(future) { _ =>
          val actualRecords = listMessagesReceivedFromSNS(topic)
            .map { _.message }
            .map { fromJson[HybridRecord](_).get }
            .distinct

          actualRecords should contain theSameElementsAs records
        }
      }
    }
  }

  it("returns a failed Future[ReindexerException] if there's an SNS error") {
    withSNSWriter(Topic("no-such-topic")) { snsWriter =>
      val bulkSNSWriter = new BulkSNSWriter(
        snsWriter = snsWriter
      )

      val future = bulkSNSWriter.sendToSNS(records = records)
      whenReady(future.failed) {
        _ shouldBe a[ReindexerException]
      }
    }
  }
}
