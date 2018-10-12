package uk.ac.wellcome.platform.sierra_items_to_dynamo

import org.scalatest.concurrent.{Eventually, IntegrationPatience}
import org.scalatest.{Assertion, FunSpec, Matchers}
import uk.ac.wellcome.messaging.test.fixtures.SQS
import uk.ac.wellcome.models.transformable.sierra.test.utils.SierraGenerators
import uk.ac.wellcome.storage.fixtures.LocalVersionedHybridStore
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.messaging.test.fixtures.SNS.Topic
import uk.ac.wellcome.models.transformable.sierra.SierraItemRecord
import uk.ac.wellcome.sierra_adapter.utils.SierraAdapterHelpers
import uk.ac.wellcome.storage.fixtures.LocalDynamoDb.Table
import uk.ac.wellcome.storage.fixtures.S3.Bucket
import uk.ac.wellcome.storage.vhs.HybridRecord

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

  private def assertStoredAndSent(itemRecord: SierraItemRecord,
                                  topic: Topic,
                                  bucket: Bucket,
                                  table: Table): Assertion = {
    val hybridRecord =
      getHybridRecord(table, id = itemRecord.id.withoutCheckDigit)

    val storedItemRecord = getObjectFromS3[SierraItemRecord](
      Bucket(hybridRecord.location.namespace),
      hybridRecord.location.key)
    storedItemRecord shouldBe itemRecord

    val snsMessages = listMessagesReceivedFromSNS(topic)
    val receivedHybridRecords = snsMessages.map { messageInfo =>
      fromJson[HybridRecord](messageInfo.message).get
    }.distinct

    receivedHybridRecords should have size 1
    receivedHybridRecords.head shouldBe hybridRecord
  }
}
