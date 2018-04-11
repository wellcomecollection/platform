package uk.ac.wellcome.platform.sierra_items_to_dynamo.services

import java.time.Instant

import com.amazonaws.services.cloudwatch.AmazonCloudWatch
import com.amazonaws.services.cloudwatch.model.PutMetricDataResult
import com.gu.scanamo.{DynamoFormat, Scanamo}
import org.scalatest.concurrent.{Eventually, ScalaFutures}
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.metrics.MetricsSender
import uk.ac.wellcome.models.aws.{DynamoConfig, SQSConfig, SQSMessage}
import uk.ac.wellcome.sqs.SQSReader
import uk.ac.wellcome.test.utils.ExtendedPatience
import uk.ac.wellcome.dynamo._
import uk.ac.wellcome.models.transformable.sierra.{SierraItemRecord, SierraRecord}
import com.gu.scanamo.syntax._
import uk.ac.wellcome.utils.JsonUtil._
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import uk.ac.wellcome.exceptions.GracefulFailureException
import uk.ac.wellcome.platform.sierra_items_to_dynamo.merger.SierraItemRecordMerger
import uk.ac.wellcome.test.fixtures._
import uk.ac.wellcome.utils.JsonUtil

import scala.concurrent.duration._

class SierraItemsToDynamoWorkerServiceTest
    extends FunSpec
      with LocalDynamoDb[SierraItemRecord]
      with SQS
      with Matchers
      with Eventually
      with ExtendedPatience
      with MockitoSugar
      with Akka
      with ScalaFutures {

  override lazy val evidence: DynamoFormat[SierraItemRecord] =
    DynamoFormat[SierraItemRecord]

  case class ServiceFixtures(
    service: SierraItemsToDynamoWorkerService,
    queueUrl: String,
    tableName: String
  )

  def withSierraWorkerService[R](
    testWith: TestWith[ServiceFixtures, R]): Unit = {
    withActorSystem { actorSystem =>
      withLocalDynamoDbTable { tableName =>
        withLocalSqsQueue { queueUrl =>
          val mockPutMetricDataResult = mock[PutMetricDataResult]
          val mockCloudWatch = mock[AmazonCloudWatch]

          when(mockCloudWatch.putMetricData(any()))
            .thenReturn(mockPutMetricDataResult)
          val mockMetrics = new MetricsSender(
            "namespace",
            100 milliseconds,
            mockCloudWatch,
            actorSystem
          )

          val sierraItemsToDynamoWorkerService =
            new SierraItemsToDynamoWorkerService(
              reader =
                new SQSReader(sqsClient, SQSConfig(queueUrl, 1.second, 1)),
              system = actorSystem,
              metrics = mockMetrics,
              versionedDao = new VersionedDao(
                dynamoDbClient = dynamoDbClient,
                dynamoConfig = DynamoConfig(tableName)
              )
            )

          testWith(
            ServiceFixtures(
              service = sierraItemsToDynamoWorkerService,
              queueUrl = queueUrl,
              tableName = tableName
            ))
        }
      }
    }
  }

  it("reads a sierra record from sqs an inserts it into DynamoDb") {
    withSierraWorkerService { fixtures =>
      val id = "12345"

      val bibIds1 = List("1", "2", "3")
      val modifiedDate1 = "2002-01-01T01:01:01Z"

      val record1 = SierraItemRecord(
        id = s"$id",
        modifiedDate = modifiedDate1,
        data = sierraRecordData(
          bibIds = bibIds1,
          modifiedDate = modifiedDate1
        ),
        bibIds = bibIds1
      )

      Scanamo.put(dynamoDbClient)(fixtures.tableName)(record1)

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

      val sqsMessage = SQSMessage(
          Some("subject"),
          toJson(record2).get,
          "topic",
          "messageType",
          "timestamp"
        )

      sqsClient.sendMessage(fixtures.queueUrl, toJson(sqsMessage).get)

      val expectedBibIds = List("3", "4", "5")
      val expectedUnlinkedBibIds = List("1", "2")

      val expectedRecord = SierraItemRecordMerger.mergeItems(
              existingRecord = record1,
              updatedRecord = record2.toItemRecord.get
      )

      val expectedData = JsonUtil.toJson(expectedRecord).get

      eventually {
        Scanamo.scan[SierraItemRecord](dynamoDbClient)(fixtures.tableName) should have size 1

        val scanamoResult =
          Scanamo.get[SierraItemRecord](dynamoDbClient)(fixtures.tableName)(
            'id -> id)

        scanamoResult shouldBe defined
        scanamoResult.get shouldBe Right(
          SierraItemRecord(id = id,
            data = expectedData,
            modifiedDate = modifiedDate2,
            bibIds = expectedBibIds,
            unlinkedBibIds = expectedUnlinkedBibIds,
            version = 1
          ))
      }
    }
  }

  it("returns a GracefulFailureException if it receives an invalid message") {
    withSierraWorkerService { fixtures =>
      val message =
        """
          |{
          | "something": "something"
          |}
        """.stripMargin

      val sqsMessage =
        SQSMessage(
          Some("subject"),
          message,
          "topic",
          "messageType",
          "timestamp")

      whenReady(fixtures.service.processMessage(sqsMessage).failed) { ex =>
        ex shouldBe a[GracefulFailureException]
      }
    }
  }

  private def sierraRecordData(bibIds: List[String] = List(),
                               unlinkedBibIds: List[String] = List(),
                               modifiedDate: String = "2001-01-01T01:01:01Z"
                          ): String = {

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
