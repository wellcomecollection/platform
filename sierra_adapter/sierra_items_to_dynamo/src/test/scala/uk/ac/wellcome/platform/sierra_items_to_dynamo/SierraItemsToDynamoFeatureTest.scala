package uk.ac.wellcome.platform.sierra_items_to_dynamo

import org.scalatest.concurrent.Eventually
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.messaging.fixtures.SQS
import uk.ac.wellcome.models.transformable.sierra.SierraItemRecord
import uk.ac.wellcome.models.transformable.sierra.test.utils.SierraUtil
import uk.ac.wellcome.storage.fixtures.LocalVersionedHybridStore
import uk.ac.wellcome.test.utils.ExtendedPatience
import uk.ac.wellcome.json.JsonUtil._

class SierraItemsToDynamoFeatureTest
    extends FunSpec
    with LocalVersionedHybridStore
    with SQS
    with fixtures.Server
    with Matchers
    with Eventually
    with ExtendedPatience
    with SierraUtil {

  it("reads items from Sierra and adds them to DynamoDB") {
    withLocalDynamoDbTable { table =>
      withLocalS3Bucket { bucket =>
        withLocalSqsQueue { queue =>
          val flags = sqsLocalFlags(queue) ++ vhsLocalFlags(bucket, table)

          withServer(flags) { server =>
            val sierraRecord = createSierraItemRecordWith(
              bibIds = List(createSierraBibNumber)
            )

            sendNotificationToSQS(
              queue = queue,
              message = sierraRecord
            )

            eventually {
              assertStored[SierraItemRecord](
                bucket = bucket,
                table = table,
                id = sierraRecord.id.withoutCheckDigit,
                record = sierraRecord
              )
            }
          }
        }
      }
    }
  }
}
