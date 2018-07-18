package uk.ac.wellcome.platform.sierra_items_to_dynamo.services

import java.time.Instant

import com.gu.scanamo.Scanamo
import com.gu.scanamo.syntax._
import org.mockito.Mockito.{never, verify}
import org.scalatest.concurrent.{Eventually, ScalaFutures}
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.messaging.sns.NotificationMessage
import uk.ac.wellcome.messaging.test.fixtures.SQS
import uk.ac.wellcome.messaging.test.fixtures.SQS.QueuePair
import uk.ac.wellcome.models.transformable.sierra.{
  SierraItemRecord,
  SierraRecordNumber
}
import uk.ac.wellcome.models.transformable.sierra.test.utils.SierraUtil
import uk.ac.wellcome.monitoring.MetricsSender
import uk.ac.wellcome.monitoring.test.fixtures.MetricsSenderFixture
import uk.ac.wellcome.platform.sierra_items_to_dynamo.fixtures.DynamoInserterFixture
import uk.ac.wellcome.platform.sierra_items_to_dynamo.merger.SierraItemRecordMerger
import uk.ac.wellcome.sierra_adapter.models.SierraRecord
import uk.ac.wellcome.sierra_adapter.test.util.SierraRecordUtil
import uk.ac.wellcome.storage.fixtures.LocalDynamoDb.Table
import uk.ac.wellcome.test.fixtures._
import uk.ac.wellcome.test.utils.ExtendedPatience
import uk.ac.wellcome.utils.JsonUtil
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
    with SierraRecordUtil
    with SierraUtil {

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

  it("reads a sierra record from sqs an inserts it into DynamoDb") {
    withSierraWorkerService {
      case (_, QueuePair(queue, _), table, _) =>
        val bibIds1 = List("1111111", "2222222", "3333333").map {
          SierraRecordNumber
        }

        val record1 = createSierraItemRecordWith(
          data = "<<older data>>",
          modifiedDate = olderDate,
          bibIds = bibIds1
        )

        Scanamo.put(dynamoDbClient)(table.name)(record1)

        val bibIds2 = List("3333333", "4444444", "5555555")

        val record2 = createSierraRecordWith(
          id = record1.id.withoutCheckDigit,
          data = sierraRecordData(
            bibIds = bibIds2,
            modifiedDate = newerDate
          ),
          modifiedDate = newerDate
        )

        sendNotificationToSQS(queue = queue, message = record2)

        val expectedBibIds = List("3333333", "4444444", "5555555").map {
          SierraRecordNumber
        }
        val expectedUnlinkedBibIds = List("1111111", "2222222").map {
          SierraRecordNumber
        }

        val expectedRecord = SierraItemRecordMerger.mergeItems(
          existingRecord = record1,
          updatedRecord = record2.toItemRecord.get
        )

        val expectedData = expectedRecord.data

        eventually {
          Scanamo.scan[SierraItemRecord](dynamoDbClient)(table.name) should have size 1

          val scanamoResult =
            Scanamo.get[SierraItemRecord](dynamoDbClient)(table.name)(
              'id -> record1.id.withoutCheckDigit)

          scanamoResult shouldBe defined
          scanamoResult.get shouldBe Right(
            SierraItemRecord(
              id = record1.id,
              data = expectedData,
              modifiedDate = newerDate,
              bibIds = expectedBibIds,
              unlinkedBibIds = expectedUnlinkedBibIds,
              version = 1))
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

  private def sierraRecordData(bibIds: List[String],
                               unlinkedBibIds: List[String] = List(),
                               modifiedDate: Instant): String = {
    val sierraItemRecord = createSierraItemRecordWith(
      id = createSierraRecordNumberString,
      modifiedDate = modifiedDate,
      data = s"""
                |{
                |  "id": "i111",
                |  "updatedDate": "${modifiedDate.toString}",
                |  "comment": "Legacy line of lamentable leopards",
                |  "bibIds": ${toJson(bibIds).get}
                |}""".stripMargin,
      bibIds = bibIds.map { SierraRecordNumber },
      unlinkedBibIds = unlinkedBibIds.map { SierraRecordNumber }
    )

    JsonUtil.toJson(sierraItemRecord).get
  }
}
