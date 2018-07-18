package uk.ac.wellcome.platform.sierra_items_to_dynamo

import java.time.Instant

import com.gu.scanamo.Scanamo
import com.gu.scanamo.syntax._
import org.scalatest.concurrent.Eventually
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.messaging.test.fixtures.SQS
import uk.ac.wellcome.models.transformable.sierra.{
  SierraItemRecord,
  SierraRecordNumber
}
import uk.ac.wellcome.sierra_adapter.models.SierraRecord
import uk.ac.wellcome.sierra_adapter.test.util.SierraRecordUtil
import uk.ac.wellcome.storage.fixtures.LocalDynamoDbVersioned
import uk.ac.wellcome.test.utils.ExtendedPatience
import uk.ac.wellcome.utils.JsonUtil._
import uk.ac.wellcome.storage.dynamo._

class SierraItemsToDynamoFeatureTest
    extends FunSpec
    with LocalDynamoDbVersioned
    with SQS
    with fixtures.Server
    with Matchers
    with Eventually
    with ExtendedPatience
    with SierraRecordUtil {

  it("reads items from Sierra and adds them to DynamoDB") {
    withLocalDynamoDbTable { table =>
      withLocalSqsQueue { queue =>
        val flags = sqsLocalFlags(queue) ++ dynamoDbLocalEndpointFlags(table)

        withServer(flags) { server =>
          val id = createSierraRecordNumberString
          val bibId = createSierraRecordNumberString
          val data = s"""{"id": "$id", "bibIds": ["$bibId"]}"""

          val record = createSierraRecordWith(id = id, data = data)

          sendNotificationToSQS(
            queue = queue,
            message = record
          )

          eventually {
            Scanamo.scan[SierraItemRecord](dynamoDbClient)(table.name) should have size 1

            val scanamoResult =
              Scanamo.get[SierraItemRecord](dynamoDbClient)(table.name)(
                'id -> id)

            scanamoResult shouldBe defined
            scanamoResult.get shouldBe Right(
              SierraItemRecord(
                id = id,
                data = data,
                modifiedDate = record.modifiedDate,
                bibIds = List(bibId).map { SierraRecordNumber },
                version = 1))
          }
        }
      }
    }
  }
}
