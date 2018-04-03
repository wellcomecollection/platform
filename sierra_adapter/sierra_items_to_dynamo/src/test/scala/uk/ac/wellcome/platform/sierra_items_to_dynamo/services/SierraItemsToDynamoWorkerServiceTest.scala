package uk.ac.wellcome.platform.sierra_items_to_dynamo.services

import java.time.Instant

import com.amazonaws.services.cloudwatch.AmazonCloudWatch
import com.amazonaws.services.cloudwatch.model.PutMetricDataResult
import com.gu.scanamo.Scanamo
import org.scalatest.concurrent.{Eventually, ScalaFutures}
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.metrics.MetricsSender
import uk.ac.wellcome.models.aws.{DynamoConfig, SQSConfig, SQSMessage}
import uk.ac.wellcome.sqs.SQSReader
import uk.ac.wellcome.test.utils.ExtendedPatience
import uk.ac.wellcome.dynamo._
import uk.ac.wellcome.models.transformable.sierra.{
  SierraItemRecord,
  SierraRecord
}
import com.gu.scanamo.syntax._
import uk.ac.wellcome.utils.JsonUtil._
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import uk.ac.wellcome.exceptions.GracefulFailureException
import uk.ac.wellcome.test.fixtures._

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
      val id = "i12345"
      val bibId = "b54321"
      val data = s"""{"id": "$id", "bibIds": ["$bibId"]}"""
      val modifiedDate = Instant.ofEpochSecond(Instant.now.getEpochSecond)
      val message = SierraRecord(id, data, modifiedDate)

      val sqsMessage =
        SQSMessage(
          Some("subject"),
          toJson(message).get,
          "topic",
          "messageType",
          "timestamp")
      sqsClient.sendMessage(fixtures.queueUrl, toJson(sqsMessage).get)

      eventually {
        Scanamo.scan[SierraItemRecord](dynamoDbClient)(fixtures.tableName) should have size 1

        val scanamoResult =
          Scanamo.get[SierraItemRecord](dynamoDbClient)(fixtures.tableName)(
            'id -> id)

        scanamoResult shouldBe defined
        scanamoResult.get shouldBe Right(
          SierraItemRecord(id, data, modifiedDate, List(bibId), version = 1))
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
}
