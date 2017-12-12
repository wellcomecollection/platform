package uk.ac.wellcome.platform.sierra_bibs_to_dynamo.services

import akka.actor.ActorSystem
import com.gu.scanamo.Scanamo
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, FunSpec, Matchers}
import org.scalatest.concurrent.{Eventually, ScalaFutures}
import org.scalatest.mockito.MockitoSugar
import uk.ac.wellcome.metrics.MetricsSender
import uk.ac.wellcome.models.aws.{DynamoConfig, SQSConfig, SQSMessage}
import uk.ac.wellcome.platform.sierra_bibs_to_dynamo.locals.SierraBibsToDynamoDBLocal
import uk.ac.wellcome.models.SierraRecord
import uk.ac.wellcome.models.SierraRecord._
import uk.ac.wellcome.sqs.{SQSReader, SQSReaderGracefulException}
import uk.ac.wellcome.test.utils.{ExtendedPatience, SQSLocal}
import uk.ac.wellcome.utils.JsonUtil

import scala.concurrent.duration._

class SierraBibsToDynamoWorkerServiceTest
    extends FunSpec
    with MockitoSugar
    with SQSLocal
    with Eventually
    with SierraBibsToDynamoDBLocal
    with Matchers
    with ExtendedPatience
    with ScalaFutures
    with BeforeAndAfterEach
    with BeforeAndAfterAll {

  val queueUrl = createQueueAndReturnUrl("sierra-test-queue")
  val mockMetrics = mock[MetricsSender]
  var worker: Option[SierraBibsToDynamoWorkerService] = None
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
      new SierraBibsToDynamoWorkerService(
        reader = new SQSReader(sqsClient, SQSConfig(queueUrl, 1.second, 1)),
        system = actorSystem,
        metrics = mockMetrics,
        dynamoDbClient = dynamoDbClient,
        apiUrl = "http://localhost:8080",
        sierraOauthKey = "key",
        sierraOauthSecret = "secret",
        dynamoConfig = DynamoConfig(tableName),
        fields = fields
      ))
  }

  it(
    "should read a window message from sqs, retrieve the bibs from sierra and insert them into DynamoDb") {
    worker = createSierraWorkerService(
      fields = "updatedDate,deletedDate,deleted,suppressed,author,title")
    worker.get.runSQSWorker()
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

  }

  it(
    "should return a SQSReaderGracefulException if it receives a message that doesn't contain start or end values") {
    worker = createSierraWorkerService(fields = "")

    val message =
      """
        |{
        | "start": "2013-12-10T17:16:35Z"
        |}
      """.stripMargin

    val sqsMessage =
      SQSMessage(Some("subject"), message, "topic", "messageType", "timestamp")
    whenReady(worker.get.processMessage(sqsMessage).failed) { ex =>
      ex shouldBe a[SQSReaderGracefulException]
    }

  }

  it(
    "should not return a SQSReaderGracefulException if it cannot reach the Sierra Api") {
    worker = Some(
      new SierraBibsToDynamoWorkerService(
        reader = new SQSReader(sqsClient, SQSConfig(queueUrl, 1.second, 1)),
        system = ActorSystem(),
        metrics = mockMetrics,
        dynamoDbClient = dynamoDbClient,
        apiUrl = "http://localhost:8081",
        sierraOauthKey = "key",
        sierraOauthSecret = "secret",
        fields = "",
        dynamoConfig = DynamoConfig(tableName)
      ))

    val message =
      """
        |{
        | "start": "2013-12-10T17:16:35Z",
        | "end": "2013-12-13T21:34:35Z"
        |}
      """.stripMargin

    val sqsMessage =
      SQSMessage(Some("subject"), message, "topic", "messageType", "timestamp")

    whenReady(worker.get.processMessage(sqsMessage).failed) { ex =>
      ex shouldNot be(a[SQSReaderGracefulException])
    }
  }

  private def stopWorker(worker: Option[SierraBibsToDynamoWorkerService]) = {
    eventually {
      worker.fold(true)(_.cancelRun()) shouldBe true
    }
  }
}
