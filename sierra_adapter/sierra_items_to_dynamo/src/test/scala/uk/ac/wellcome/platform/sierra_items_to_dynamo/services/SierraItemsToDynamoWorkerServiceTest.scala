package uk.ac.wellcome.platform.sierra_items_to_dynamo.services

import com.gu.scanamo.Scanamo
import com.gu.scanamo.syntax._
import org.mockito.Mockito.{never, verify}
import org.scalatest.concurrent.{Eventually, ScalaFutures}
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.messaging.sns.NotificationMessage
import uk.ac.wellcome.messaging.test.fixtures.SQS
import uk.ac.wellcome.messaging.test.fixtures.SQS.QueuePair
import uk.ac.wellcome.models.transformable.sierra.SierraItemRecord
import uk.ac.wellcome.models.transformable.sierra.test.utils.SierraUtil
import uk.ac.wellcome.monitoring.MetricsSender
import uk.ac.wellcome.monitoring.test.fixtures.MetricsSenderFixture
import uk.ac.wellcome.platform.sierra_items_to_dynamo.fixtures.DynamoInserterFixture
import uk.ac.wellcome.platform.sierra_items_to_dynamo.merger.SierraItemRecordMerger
import uk.ac.wellcome.sierra_adapter.models.test.utils.SierraRecordUtil
import uk.ac.wellcome.storage.test.fixtures.LocalDynamoDb.Table
import uk.ac.wellcome.test.fixtures._
import uk.ac.wellcome.test.utils.ExtendedPatience
import uk.ac.wellcome.utils.JsonUtil._
import uk.ac.wellcome.storage.dynamo._

class SierraItemsToDynamoWorkerServiceTest
    extends FunSpec
    with DynamoInserterFixture
    with SQS
    with Matchers
    with Eventually
    with ExtendedPatience
    with Akka
    with MetricsSenderFixture
    with ScalaFutures
    with SierraUtil
    with SierraRecordUtil {

  def withSierraWorkerService[R](
    testWith: TestWith[(SierraItemsToDynamoWorkerService,
                        QueuePair,
                        Table,
                        MetricsSender),
                       R]): Unit = {
    withActorSystem { actorSystem =>
      withLocalDynamoDbTable { table =>
        withDynamoInserter(table) { dynamoInserter =>
          withLocalSqsQueueAndDlq {
            case queuePair @ QueuePair(queue, dlq) =>
              withMockMetricSender { metricsSender =>
                withSQSStream[NotificationMessage, R](
                  actorSystem,
                  queue,
                  metricsSender) { sqsStream =>
                  val sierraItemsToDynamoWorkerService =
                    new SierraItemsToDynamoWorkerService(
                      system = actorSystem,
                      sqsStream = sqsStream,
                      dynamoInserter = dynamoInserter
                    )

                  testWith(
                    (
                      sierraItemsToDynamoWorkerService,
                      queuePair,
                      table,
                      metricsSender
                    ))
                }
              }
          }
        }
      }
    }
  }

  it("reads a Sierra record from SQS and inserts it into DynamoDb") {
    withSierraWorkerService {
      case (_, QueuePair(queue, _), table, _) =>
        val bibIds1 = List("1", "2", "3")
        val record1 = createSierraItemRecordWith(
          modifiedDate = olderDate,
          bibIds = bibIds1
        )

        Scanamo.put(dynamoDbClient)(table.name)(record1)

        val bibIds2 = List("3", "4", "5")
        val record2 = createSierraRecordWith(
          id = record1.id,
          data = s"""
            |{
            |  "id": "${record1.id}",
            |  "updatedDate": "${newerDate.toString}",
            |  "bibIds": ${toJson(bibIds2).get}
            |}""".stripMargin,
          modifiedDate = newerDate
        )

        sendNotificationToSQS(queue = queue, message = record2)

        val expectedRecord = SierraItemRecordMerger.mergeItems(
          existingRecord = record1,
          updatedRecord = record2.toItemRecord.get
        )

        expectedRecord.bibIds shouldBe List("3", "4", "5")
        expectedRecord.unlinkedBibIds shouldBe List("1", "2")

        eventually {
          Scanamo.scan[SierraItemRecord](dynamoDbClient)(table.name) should have size 1

          val scanamoResult =
            Scanamo.get[SierraItemRecord](dynamoDbClient)(table.name)(
              'id -> record1.id)

          scanamoResult shouldBe defined
          scanamoResult.get shouldBe Right(expectedRecord.copy(version = 1))
        }
    }
  }

  it("returns a GracefulFailureException if it receives an invalid message") {
    withSierraWorkerService {
      case (_, QueuePair(queue, dlq), _, metricsSender) =>
        val body =
          """
          |{
          | "something": "something"
          |}
        """.stripMargin

        sendNotificationToSQS(queue = queue, body = body)

        eventually {
          assertQueueEmpty(queue)
          assertQueueHasSize(dlq, 1)
          verify(metricsSender, never()).incrementCount(
            "SierraItemsToDynamoWorkerService_ProcessMessage_failure")
        }
    }
  }
}
