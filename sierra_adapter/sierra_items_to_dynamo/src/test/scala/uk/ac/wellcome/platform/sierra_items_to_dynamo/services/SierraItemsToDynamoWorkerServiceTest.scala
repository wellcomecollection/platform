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
import uk.ac.wellcome.models.transformable.sierra.SierraItemRecord
import uk.ac.wellcome.monitoring.MetricsSender
import uk.ac.wellcome.monitoring.test.fixtures.MetricsSenderFixture
import uk.ac.wellcome.platform.sierra_items_to_dynamo.fixtures.DynamoInserterFixture
import uk.ac.wellcome.platform.sierra_items_to_dynamo.merger.SierraItemRecordMerger
import uk.ac.wellcome.sierra_adapter.models.SierraRecord
import uk.ac.wellcome.storage.test.fixtures.LocalDynamoDb.Table
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
    with ScalaFutures {

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
        val id = "12345"

        val bibIds1 = List("1", "2", "3")
        val modifiedDate1 = "2001-01-01T01:01:01Z"

        val record1 = SierraItemRecord(
          id = s"$id",
          modifiedDate = modifiedDate1,
          data = sierraRecordData(
            bibIds = bibIds1,
            modifiedDate = modifiedDate1
          ),
          bibIds = bibIds1
        )

        Scanamo.put(dynamoDbClient)(table.name)(record1)

        val bibIds2 = List("3", "4", "5")
        val modifiedDate2 = Instant.parse("2002-01-01T01:01:01Z")

        val record2 = SierraRecord(
          id = id,
          data = sierraRecordData(
            bibIds = bibIds2,
            modifiedDate = modifiedDate2.toString
          ),
          modifiedDate = modifiedDate2
        )

        val sqsMessage = NotificationMessage(
          MessageId = "message-id",
          TopicArn = "topic",
          Subject = "subject",
          Message = toJson(record2).get
        )

        sqsClient.sendMessage(queue.url, toJson(sqsMessage).get)

        val expectedBibIds = List("3", "4", "5")
        val expectedUnlinkedBibIds = List("1", "2")

        val expectedRecord = SierraItemRecordMerger.mergeItems(
          existingRecord = record1,
          updatedRecord = record2.toItemRecord.get
        )

        val expectedData = expectedRecord.data

        eventually {
          Scanamo.scan[SierraItemRecord](dynamoDbClient)(table.name) should have size 1

          val scanamoResult =
            Scanamo.get[SierraItemRecord](dynamoDbClient)(table.name)('id -> id)

          scanamoResult shouldBe defined
          scanamoResult.get shouldBe Right(
            SierraItemRecord(
              id = id,
              data = expectedData,
              modifiedDate = modifiedDate2,
              bibIds = expectedBibIds,
              unlinkedBibIds = expectedUnlinkedBibIds,
              version = 1))
        }
    }
  }

  it("returns a GracefulFailureException if it receives an invalid message") {
    withSierraWorkerService {
      case (_, QueuePair(queue, dlq), _, metricsSender) =>
        val message =
          """
          |{
          | "something": "something"
          |}
        """.stripMargin

        val sqsMessage = NotificationMessage(
          MessageId = "message-id",
          TopicArn = "topic",
          Subject = "subject",
          Message = message
        )

        sqsClient.sendMessage(queue.url, toJson(sqsMessage).get)

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
                               modifiedDate: String): String = {

    val sierraItemRecord = SierraItemRecord(
      id = s"i111",
      modifiedDate = Instant.parse(modifiedDate),
      data = s"""
                |{
                |  "id": "i111",
                |  "updatedDate": "$modifiedDate",
                |  "comment": "Legacy line of lamentable leopards",
                |  "bibIds": ${toJson(bibIds).get}
                |}""".stripMargin,
      bibIds = bibIds,
      unlinkedBibIds = unlinkedBibIds
    )

    JsonUtil.toJson(sierraItemRecord).get
  }
}
