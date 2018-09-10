package uk.ac.wellcome.platform.sierra_items_to_dynamo

import org.scalatest.concurrent.{Eventually, IntegrationPatience}
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.messaging.test.fixtures.SQS
import uk.ac.wellcome.models.transformable.sierra.test.utils.SierraGenerators
import uk.ac.wellcome.storage.fixtures.LocalVersionedHybridStore
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.sierra_adapter.utils.SierraAdapterHelpers

class SierraItemsToDynamoFeatureTest
    extends FunSpec
    with LocalVersionedHybridStore
    with SQS
    with fixtures.Server
    with Matchers
    with Eventually
    with IntegrationPatience
    with SierraAdapterHelpers
    with SierraGenerators {

  it("reads items from Sierra and adds them to DynamoDB") {
    withLocalDynamoDbTable { table =>
      withLocalS3Bucket { bucket =>
        withLocalSqsQueue { queue =>
          withLocalSnsTopic { topic =>
            val flags = sqsLocalFlags(queue) ++ vhsLocalFlags(bucket, table) ++ snsLocalFlags(
              topic)

            withServer(flags) { server =>
              val itemRecord = createSierraItemRecordWith(
                bibIds = List(createSierraBibNumber)
              )

              sendNotificationToSQS(
                queue = queue,
                message = itemRecord
              )

              eventually {
                assertStoredAndSent(
                  itemRecord = itemRecord,
                  topic = topic,
                  bucket = bucket,
                  table = table
                )
              }
            }
          }
        }
      }
    }
  }
}
