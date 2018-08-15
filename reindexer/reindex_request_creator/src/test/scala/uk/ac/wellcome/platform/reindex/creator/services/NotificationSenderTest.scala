package uk.ac.wellcome.platform.reindex.creator.services

import com.amazonaws.services.sns.model.AmazonSNSException
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.messaging.test.fixtures.SNS
import uk.ac.wellcome.messaging.test.fixtures.SNS.Topic
import uk.ac.wellcome.models.reindexer.ReindexRequest
import uk.ac.wellcome.platform.reindex.creator.fixtures.ReindexFixtures
import uk.ac.wellcome.storage.fixtures.LocalDynamoDb.Table
import uk.ac.wellcome.json.JsonUtil._

import scala.concurrent.ExecutionContext.Implicits.global

class NotificationSenderTest
    extends FunSpec
    with Matchers
    with ReindexFixtures
    with ScalaFutures
    with IntegrationPatience
    with SNS {
  it("sends ReindexRequests for the provided IDs") {
    withLocalSnsTopic { topic =>
      withSNSWriter(topic) { snsWriter =>
        val notificationSender = new NotificationSender(
          snsWriter = snsWriter
        )

        val recordIds = List("miro/1", "miro/2", "miro/3")
        val desiredVersion = 5

        val table = Table("my-test-table", "my-index")

        val expectedRequests = recordIds.map { id =>
          ReindexRequest(
            id = id,
            desiredVersion = desiredVersion,
            tableName = table.name
          )
        }

        val future = notificationSender.sendNotifications(
          recordIds = recordIds,
          reindexJob = createReindexJobWith(
            table = table,
            desiredVersion = desiredVersion
          )
        )

        whenReady(future) { _ =>
          val actualRequests = listMessagesReceivedFromSNS(topic)
            .map {
              _.message
            }
            .map {
              fromJson[ReindexRequest](_).get
            }
            .distinct

          actualRequests should contain theSameElementsAs expectedRequests
        }
      }
    }
  }

  it("returns a failed Future if there's an SNS error") {
    withSNSWriter(Topic("no-such-topic")) { snsWriter =>
      val notificationSender = new NotificationSender(
        snsWriter = snsWriter
      )

      val future = notificationSender.sendNotifications(
        recordIds = List("1", "2", "3"),
        reindexJob = createReindexJobWith(
          table = Table("my-test-table", "my-index")
        )
      )
      whenReady(future.failed) {
        _ shouldBe a[AmazonSNSException]
      }
    }
  }
}
