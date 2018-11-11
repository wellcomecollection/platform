package uk.ac.wellcome.platform.sierra_items_to_dynamo

import org.scalatest.concurrent.{Eventually, IntegrationPatience}
import org.scalatest.{Assertion, FunSpec, Matchers}
import uk.ac.wellcome.messaging.test.fixtures.SQS
import uk.ac.wellcome.models.transformable.sierra.test.utils.SierraGenerators
import uk.ac.wellcome.storage.fixtures.LocalVersionedHybridStore
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.messaging.sns.NotificationMessage
import uk.ac.wellcome.messaging.test.fixtures.SNS.Topic
import uk.ac.wellcome.messaging.test.fixtures.SQS.Queue
import uk.ac.wellcome.models.transformable.sierra.SierraItemRecord
import uk.ac.wellcome.platform.sierra_items_to_dynamo.fixtures.DynamoInserterFixture
import uk.ac.wellcome.platform.sierra_items_to_dynamo.services.SierraItemsToDynamoWorkerService
import uk.ac.wellcome.sierra_adapter.utils.SierraAdapterHelpers
import uk.ac.wellcome.storage.fixtures.LocalDynamoDb.Table
import uk.ac.wellcome.storage.fixtures.S3.Bucket
import uk.ac.wellcome.test.fixtures.TestWith

import scala.concurrent.ExecutionContext.Implicits.global

class SierraItemsToDynamoFeatureTest
    extends FunSpec
    with LocalVersionedHybridStore
    with SQS
    with Matchers
    with Eventually
    with IntegrationPatience
    with DynamoInserterFixture
    with SierraAdapterHelpers
    with SierraGenerators {

  it("reads items from Sierra and adds them to DynamoDB") {
    withLocalDynamoDbTable { table =>
      withLocalS3Bucket { bucket =>
        withLocalSqsQueue { queue =>
          withLocalSnsTopic { topic =>
            withWorkerService(queue, table, bucket, topic) { service =>
              service.run()

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

  private def withWorkerService[R](
    queue: Queue,
    table: Table,
    bucket: Bucket,
    topic: Topic)(testWith: TestWith[SierraItemsToDynamoWorkerService, R]): R =
    withActorSystem { actorSystem =>
      withSQSStream[NotificationMessage, R](actorSystem, queue) { sqsStream =>
        withDynamoInserter(table, bucket) { dynamoInserter =>
          withSNSWriter(topic) { snsWriter =>
            val workerService = new SierraItemsToDynamoWorkerService(
              sqsStream = sqsStream,
              dynamoInserter = dynamoInserter,
              snsWriter = snsWriter
            )

            testWith(workerService)
          }
        }
      }
    }

  private def assertStoredAndSent(itemRecord: SierraItemRecord,
                                  topic: Topic,
                                  bucket: Bucket,
                                  table: Table): Assertion =
    assertStoredAndSent[SierraItemRecord](
      itemRecord,
      id = itemRecord.id.withoutCheckDigit,
      topic = topic,
      bucket = bucket,
      table = table
    )
}
