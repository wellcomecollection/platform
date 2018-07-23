package uk.ac.wellcome.platform.sierra_items_to_dynamo

import com.gu.scanamo.Scanamo
import com.gu.scanamo.syntax._
import org.scalatest.concurrent.Eventually
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.messaging.test.fixtures.SQS
import uk.ac.wellcome.models.transformable.sierra.SierraItemRecord
import uk.ac.wellcome.sierra_adapter.test.utils.SierraRecordUtil
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
          val itemId = createSierraRecordNumberString
          val bibId = createSierraRecordNumberString
          val data = s"""{"id": "$itemId", "bibIds": ["$bibId"]}"""

          sendNotificationToSQS(
            queue = queue,
            message = createSierraRecordWith(
              id = itemId,
              data = data
            )
          )

          eventually {
            Scanamo.scan[SierraItemRecord](dynamoDbClient)(table.name) should have size 1

            val scanamoResult =
              Scanamo.get[SierraItemRecord](dynamoDbClient)(table.name)(
                'id -> itemId)

            scanamoResult shouldBe defined
            scanamoResult.get shouldBe Right(
              SierraItemRecord(
                itemId,
                data,
                modifiedDate,
                List(bibId),
                version = 1))
          }
        }
      }
    }
  }
}
