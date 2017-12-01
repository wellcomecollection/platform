package uk.ac.wellcome.platform.sierra_to_dynamo.services

import akka.actor.ActorSystem
import com.gu.scanamo.Scanamo
import org.scalatest.{FunSpec, Matchers}
import org.scalatest.concurrent.{Eventually, ScalaFutures}
import org.scalatest.mockito.MockitoSugar
import uk.ac.wellcome.metrics.MetricsSender
import uk.ac.wellcome.models.aws.{DynamoConfig, SQSConfig, SQSMessage}
import uk.ac.wellcome.platform.sierra_to_dynamo.locals.SierraDynamoDBLocal
import uk.ac.wellcome.models.SierraRecord
import uk.ac.wellcome.models.SierraRecord._
import uk.ac.wellcome.sqs.{SQSReader, SQSReaderGracefulException}
import uk.ac.wellcome.test.utils.{ExtendedPatience, SQSLocal}
import uk.ac.wellcome.utils.JsonUtil

import scala.concurrent.duration._

class SierraToDynamoWorkerServiceTest
    extends FunSpec
    with MockitoSugar
    with SQSLocal
    with Eventually
    with SierraDynamoDBLocal
    with Matchers
    with ExtendedPatience
    with ScalaFutures{

  val queueUrl = createQueueAndReturnUrl("sierra-test-queue")
  val mockMetrics = mock[MetricsSender]

  private def createSierraWorkerService(resourceType: String, fields: String) = {
    new SierraToDynamoWorkerService(
      reader = new SQSReader(sqsClient, SQSConfig(queueUrl, 1.second, 1)),
      system = ActorSystem(),
      metrics = mockMetrics,
      dynamoDbClient = dynamoDbClient,
      apiUrl = "http://localhost:8080",
      sierraOauthKey = "key",
      sierraOauthSecret = "secret",
      resourceType = resourceType,
      dynamoConfig = DynamoConfig(tableName),
      fields = fields
    )
  }

  it("should read a window message from sqs, retrieve the items from sierra and insert into DynamoDb") {
    val worker = createSierraWorkerService("items","updatedDate,deleted,deletedDate,bibIds,fixedFields,varFields")
    worker.runSQSWorker()
    val message =
      """
        |{
        | "start": "2013-12-10T17:16:35Z",
        | "end": "2013-12-13T21:34:35Z"
        |}
      """.stripMargin

    val sqsMessage =
      SQSMessage(Some("subject"), message, "topic", "messageType", "timestamp")
    sqsClient.sendMessage(queueUrl, JsonUtil.toJson(sqsMessage).get)

    eventually {
      // This comes from the wiremock recordings for sierra api response
      Scanamo.scan[SierraRecord](dynamoDbClient)(tableName) should have size 157
    }
    worker.cancelRun()
  }

  it("should read a window message from sqs, retrieve the bibs from sierra and insert them into DynamoDb") {
    val worker = createSierraWorkerService("bibs", "updatedDate,deletedDate,deleted,suppressed,author,title")
    worker.runSQSWorker()
    val message =
      """
        |{
        | "start": "2013-12-10T17:16:35Z",
        | "end": "2013-12-13T21:34:35Z"
        |}
      """.stripMargin

    val sqsMessage =
      SQSMessage(Some("subject"), message, "topic", "messageType", "timestamp")
    sqsClient.sendMessage(queueUrl, JsonUtil.toJson(sqsMessage).get)

    eventually {
      // This comes from the wiremock recordings for sierra api response
      Scanamo.scan[SierraRecord](dynamoDbClient)(tableName) should have size 29
    }
    worker.cancelRun()
  }

  it("should return a SQSReaderGracefulException if it receives a message that doesn't contain start or end values") {
    val worker = createSierraWorkerService("items", "")

    val message =
      """
        |{
        | "start": "2013-12-10T17:16:35Z"
        |}
      """.stripMargin

    val sqsMessage =
      SQSMessage(Some("subject"), message, "topic", "messageType", "timestamp")
    whenReady(worker.processMessage(sqsMessage).failed) {ex =>
      ex shouldBe a [SQSReaderGracefulException]
    }

  }

  it("should not return a SQSReaderGracefulException if it cannot reach the Sierra Api") {
    val worker = new SierraToDynamoWorkerService(
      reader = new SQSReader(sqsClient, SQSConfig(queueUrl, 1.second, 1)),
      system = ActorSystem(),
      metrics = mockMetrics,
      dynamoDbClient = dynamoDbClient,
      apiUrl = "http://localhost:8081",
      sierraOauthKey = "key",
      sierraOauthSecret = "secret",
      resourceType = "items",
      fields = "",
      dynamoConfig = DynamoConfig(tableName)
    )

    val message =
      """
        |{
        | "start": "2013-12-10T17:16:35Z",
        | "end": "2013-12-13T21:34:35Z"
        |}
      """.stripMargin

    val sqsMessage =
      SQSMessage(Some("subject"), message, "topic", "messageType", "timestamp")

    whenReady(worker.processMessage(sqsMessage).failed) {ex =>
      ex shouldNot be (a[SQSReaderGracefulException])
    }
  }
}
