package uk.ac.wellcome.platform.reindex.processor

import com.gu.scanamo.Scanamo
import com.gu.scanamo.syntax._
import org.scalatest.FunSpec
import uk.ac.wellcome.messaging.test.fixtures.SQS
import uk.ac.wellcome.models.reindexer.{ReindexRequest, ReindexableRecord}
import uk.ac.wellcome.storage.fixtures.LocalDynamoDbVersioned
import uk.ac.wellcome.test.utils.ExtendedPatience
import uk.ac.wellcome.json.JsonUtil._

class ReindexProcessorFeatureTest
    extends FunSpec
    with fixtures.Server
    with SQS
    with LocalDynamoDbVersioned
    with ExtendedPatience {

  it("processes a ReindexRequest") {
    withLocalSqsQueue { queue =>
      withLocalDynamoDbTable { table =>
        withServer(sqsLocalFlags(queue) ++ dynamoClientLocalFlags) {
          _ =>
            val id = "sierra/1234567"
            val record =
              ReindexableRecord(id = id, version = 1, reindexVersion = 10)

            givenTableHasItem(record, table)

            val reindexRequest = ReindexRequest(
              id = id,
              tableName = table.name,
              desiredVersion = 11
            )

            sendNotificationToSQS(queue, reindexRequest)

            eventually {
              assertQueueEmpty(queue)
              val actualRecord = Scanamo.get[ReindexableRecord](dynamoDbClient)(
                table.name)('id -> id)
              val expectedRecord = record.copy(
                version = record.version + 1,
                reindexVersion = reindexRequest.desiredVersion
              )
              actualRecord shouldBe Some(Right(expectedRecord))
            }
        }
      }
    }
  }

}
