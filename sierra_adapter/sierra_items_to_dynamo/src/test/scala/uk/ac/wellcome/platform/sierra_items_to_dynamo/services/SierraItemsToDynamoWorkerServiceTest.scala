package uk.ac.wellcome.platform.sierra_items_to_dynamo.services

import java.time.Instant

import akka.actor.ActorSystem
import com.amazonaws.services.cloudwatch.AmazonCloudWatch
import com.amazonaws.services.cloudwatch.model.PutMetricDataResult
import com.gu.scanamo.Scanamo
import org.scalatest.concurrent.{Eventually, ScalaFutures}
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, FunSpec, Matchers}
import uk.ac.wellcome.metrics.MetricsSender
import uk.ac.wellcome.models.aws.{DynamoConfig, SQSConfig, SQSMessage}
import uk.ac.wellcome.platform.sierra_items_to_dynamo.locals.SierraItemsToDynamoDBLocal
import uk.ac.wellcome.sqs.SQSReader
import uk.ac.wellcome.test.utils.{ExtendedPatience, SQSLocal}
import uk.ac.wellcome.dynamo._
import uk.ac.wellcome.models.transformable.sierra.{
  SierraBibRecord,
  SierraItemRecord,
  SierraRecord
}
import uk.ac.wellcome.platform.sierra_items_to_dynamo.dynamo.SierraItemRecordDao
import com.gu.scanamo.syntax._
import uk.ac.wellcome.utils.JsonUtil._
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import uk.ac.wellcome.exceptions.GracefulFailureException

import scala.concurrent.duration._

class SierraItemsToDynamoWorkerServiceTest
    extends FunSpec
    with MockitoSugar
    with SQSLocal
    with Eventually
    with SierraItemsToDynamoDBLocal
    with Matchers
    with ExtendedPatience
    with ScalaFutures
    with BeforeAndAfterEach
    with BeforeAndAfterAll {

  val queueUrl = createQueueAndReturnUrl("sierra-test-queue")
  val mockPutMetricDataResult = mock[PutMetricDataResult]
  val mockCloudWatch = mock[AmazonCloudWatch]

  when(mockCloudWatch.putMetricData(any())).thenReturn(mockPutMetricDataResult)
  val mockMetrics =
    new MetricsSender("namespace", mockCloudWatch, ActorSystem())

  var worker: Option[SierraItemsToDynamoWorkerService] = None
  val actorSystem = ActorSystem()

  override def beforeEach(): Unit = {
    super.beforeEach()
    stopWorker(worker)
  }

  override def afterAll(): Unit = {
    super.afterAll()
    actorSystem.terminate()
  }

  private def createSierraWorkerService(fields: String) = {
    Some(
      new SierraItemsToDynamoWorkerService(
        reader = new SQSReader(sqsClient, SQSConfig(queueUrl, 1.second, 1)),
        system = actorSystem,
        metrics = mockMetrics,
        dynamoInserter = new DynamoInserter(
          sierraItemRecordDao = new SierraItemRecordDao(
            dynamoDbClient,
            ynamoConfig(tableName)
          )
        )
      ))
  }

  it("reads a sierra record from sqs an inserts it into DynamoDb") {
    worker = createSierraWorkerService(
      fields = "updatedDate,deleted,deletedDate,bibIds,fixedFields,varFields")
    worker.get.runSQSWorker()
    val id = "i12345"
    val bibId = "b54321"
    val data = s"""{"id": "$id", "bibIds": ["$bibId"]}"""
    val modifiedDate = Instant.ofEpochSecond(Instant.now.getEpochSecond)
    val message = SierraRecord(id, data, modifiedDate)

    val sqsMessage =
      SQSMessage(Some("subject"),
                 toJson(message).get,
                 "topic",
                 "messageType",
                 "timestamp")
    sqsClient.sendMessage(queueUrl, toJson(sqsMessage).get)

    eventually {
      Scanamo.scan[SierraItemRecord](dynamoDbClient)(tableName) should have size 1
      val scanamoResult =
        Scanamo.get[SierraItemRecord](dynamoDbClient)(tableName)('id -> id)
      scanamoResult shouldBe defined
      scanamoResult.get shouldBe Right(
        SierraItemRecord(id, data, modifiedDate, List(bibId), version = 1))
    }
  }

  it("returns a GracefulFailureException if it receives an invalid message") {
    worker = createSierraWorkerService(fields = "")

    val message =
      """
        |{
        | "something": "something"
        |}
      """.stripMargin

    val sqsMessage =
      SQSMessage(Some("subject"), message, "topic", "messageType", "timestamp")
    whenReady(worker.get.processMessage(sqsMessage).failed) { ex =>
      ex shouldBe a[GracefulFailureException]
    }

  }

  private def stopWorker(worker: Option[SierraItemsToDynamoWorkerService]) = {
    eventually {
      worker.fold(true)(_.cancelRun()) shouldBe true
    }
  }
}
