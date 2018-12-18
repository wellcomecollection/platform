package uk.ac.wellcome.platform.sierra_items_to_dynamo

import org.scalatest.concurrent.{Eventually, IntegrationPatience}
import org.scalatest.{Assertion, FunSpec, Matchers}
import uk.ac.wellcome.models.transformable.sierra.test.utils.SierraGenerators
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.messaging.fixtures.SNS.Topic
import uk.ac.wellcome.models.transformable.sierra.SierraItemRecord
import uk.ac.wellcome.platform.sierra_items_to_dynamo.fixtures.WorkerServiceFixture
import uk.ac.wellcome.sierra_adapter.utils.SierraAdapterHelpers
import uk.ac.wellcome.storage.fixtures.LocalDynamoDb.Table

class SierraItemsToDynamoFeatureTest
    extends FunSpec
    with Matchers
    with Eventually
    with IntegrationPatience
    with SierraAdapterHelpers
    with SierraGenerators
    with WorkerServiceFixture {

  it("reads items from Sierra and adds them to DynamoDB") {
    withLocalDynamoDbTable { table =>
      withLocalS3Bucket { bucket =>
        withLocalSqsQueue { queue =>
          withLocalSnsTopic { topic =>
            withWorkerService(queue, table, bucket, topic) { _ =>
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
                  table = table
                )
              }
            }
          }
        }
      }
    }
  }

  private def assertStoredAndSent(itemRecord: SierraItemRecord,
                                  topic: Topic,
                                  table: Table): Assertion =
    assertStoredAndSent[SierraItemRecord](
      itemRecord,
      id = itemRecord.id.withoutCheckDigit,
      topic = topic,
      table = table
    )
}
