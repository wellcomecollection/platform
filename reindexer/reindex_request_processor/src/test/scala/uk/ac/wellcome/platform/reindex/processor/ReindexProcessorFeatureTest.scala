package uk.ac.wellcome.platform.reindex.processor

import com.gu.scanamo.Scanamo
import com.gu.scanamo.syntax._
import org.scalatest.FunSpec
import uk.ac.wellcome.messaging.sns.NotificationMessage
import uk.ac.wellcome.messaging.test.fixtures.SQS
import uk.ac.wellcome.models.reindexer.{ReindexRequest, ReindexableRecord}
import uk.ac.wellcome.storage.test.fixtures.LocalDynamoDbVersioned
import uk.ac.wellcome.test.utils.ExtendedPatience
import uk.ac.wellcome.utils.JsonUtil._

class ReindexProcessorFeatureTest
    extends FunSpec
    with fixtures.Server
    with SQS
    with LocalDynamoDbVersioned
    with ExtendedPatience {

  it("processes a ReindexRequest") {
    withLocalSqsQueue { queue =>
      withLocalDynamoDbTable { table =>
        val flags = sqsLocalFlags(queue) ++ dynamoDbLocalEndpointFlags(table)
        withServer(flags) { _ =>
          val id = "sierra/1234567"
          val record =
            ReindexableRecord(id = id, version = 1, reindexVersion = 10)
          Scanamo.put(dynamoDbClient)(table.name)(record)

          val reindexRequest = ReindexRequest(id = id, desiredVersion = 11)
          val message = toJson(
            NotificationMessage(
              "snsID",
              "snsTopic",
              "snsSubject",
              toJson(reindexRequest).get)).get

          sqsClient.sendMessage(queue.url, message)

          eventually {
            assertQueueEmpty(queue)
            val actualRecord = Scanamo.get[ReindexableRecord](dynamoDbClient)(
              table.name)('id -> id)
            actualRecord shouldBe Some(
              Right(record.copy(version = 2, reindexVersion = 11)))
          }
        }
      }
    }
  }

}
