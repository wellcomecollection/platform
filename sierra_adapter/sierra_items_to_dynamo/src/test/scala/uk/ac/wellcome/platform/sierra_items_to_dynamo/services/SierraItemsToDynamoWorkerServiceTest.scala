package uk.ac.wellcome.platform.sierra_items_to_dynamo.services

import java.time.Instant

import akka.actor.ActorSystem
import com.gu.scanamo.Scanamo
import org.scalatest.concurrent.{Eventually, ScalaFutures}
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, FunSpec, Matchers}
import uk.ac.wellcome.metrics.MetricsSender
import uk.ac.wellcome.models.aws.{DynamoConfig, SQSConfig, SQSMessage}
import uk.ac.wellcome.platform.sierra_items_to_dynamo.locals.SierraItemsToDynamoDBLocal
import uk.ac.wellcome.sqs.{SQSReader, SQSReaderGracefulException}
import uk.ac.wellcome.test.utils.{ExtendedPatience, SQSLocal}
import uk.ac.wellcome.utils.JsonUtil
import uk.ac.wellcome.dynamo._
import uk.ac.wellcome.models.transformable.sierra.{
  SierraBibRecord,
  SierraItemRecord,
  SierraRecord
}
import uk.ac.wellcome.platform.sierra_items_to_dynamo.dynamo.SierraItemRecordDao
import io.circe.generic.auto._
import io.circe.syntax._
import com.gu.scanamo.syntax._
import uk.ac.wellcome.circe._

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
  val mockMetrics = mock[MetricsSender]
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
            Map("sierraToDynamo" -> DynamoConfig(tableName)))
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
    val modifiedDate = Instant.now
    val message = SierraRecord(id, data, modifiedDate)

    val sqsMessage =
      SQSMessage(Some("subject"),
                 message.asJson.noSpaces,
                 "topic",
                 "messageType",
                 "timestamp")
    sqsClient.sendMessage(queueUrl, sqsMessage.asJson.noSpaces)

    eventually {
      Scanamo.scan[SierraItemRecord](dynamoDbClient)(tableName) should have size 1
      Scanamo.get[SierraItemRecord](dynamoDbClient)(tableName)('id -> id) shouldBe SierraItemRecord(
        id,
        data,
        modifiedDate,
        List(bibId))
    }
  }

  private def stopWorker(worker: Option[SierraItemsToDynamoWorkerService]) = {
    eventually {
      worker.fold(true)(_.cancelRun()) shouldBe true
    }
  }
}
