package uk.ac.wellcome.platform.sierra_bibs_to_sns.services

import akka.actor.ActorSystem
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, FunSpec, Matchers}
import org.scalatest.concurrent.{Eventually, ScalaFutures}
import org.scalatest.mockito.MockitoSugar
import uk.ac.wellcome.metrics.MetricsSender
import uk.ac.wellcome.models.aws.{SNSConfig, SQSConfig, SQSMessage}
import uk.ac.wellcome.sns.SNSWriter
import uk.ac.wellcome.sqs.{SQSReader, SQSReaderGracefulException}
import uk.ac.wellcome.test.utils.{ExtendedPatience, SNSLocal, SQSLocal}
import uk.ac.wellcome.utils.JsonUtil

import scala.concurrent.duration._

class SierraBibsToSnsWorkerServiceTest
    extends FunSpec
    with MockitoSugar
    with SNSLocal
    with SQSLocal
    with Eventually
    with Matchers
    with ExtendedPatience
    with ScalaFutures
    with BeforeAndAfterEach
    with BeforeAndAfterAll {

  val queueUrl = createQueueAndReturnUrl("sierra-test-queue")
  val topicArn = createTopicAndReturnArn("sierra-test-topic")

  val mockMetrics = mock[MetricsSender]
  var worker: Option[SierraBibsToSnsWorkerService] = None
  val actorSystem = ActorSystem()

  override def beforeEach(): Unit = {
    super.beforeEach()
    stopWorker(worker)
  }

  override def afterAll(): Unit = {
    super.afterAll()
    actorSystem.terminate()
  }

  private def createSierraBibsToSnsWorkerService(
    fields: String,
    apiUrl: String = "http://localhost:8080"
  ) = {
    Some(
      new SierraBibsToSnsWorkerService(
        reader = new SQSReader(sqsClient, SQSConfig(queueUrl, 1.second, 1)),
        writer = new SNSWriter(snsClient, SNSConfig(topicArn = topicArn)),
        system = actorSystem,
        metrics = mockMetrics,
        apiUrl = apiUrl,
        sierraOauthKey = "key",
        sierraOauthSecret = "secret",
        fields = fields
      ))
  }

  it(
    "reads a window message from SQS, retrieves the bibs from Sierra and sends them to SNS") {
    worker = createSierraBibsToSnsWorkerService(
      fields = "updatedDate,deletedDate,deleted,suppressed,author,title"
    )
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
      // This comes from the wiremock recordings for Sierra API responses
      listMessagesReceivedFromSNS() should have size 29
    }

  }

  it(
    "returns a SQSReaderGracefulException if it receives a message that doesn't contain start or end values") {
    worker = createSierraBibsToSnsWorkerService(fields = "")

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
    "does not return a SQSReaderGracefulException if it cannot reach the Sierra API") {
    worker = createSierraBibsToSnsWorkerService(
      fields = "",
      apiUrl = "http://localhost:5050"
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

    whenReady(worker.get.processMessage(sqsMessage).failed) { ex =>
      ex shouldNot be(a[SQSReaderGracefulException])
    }
  }

  private def stopWorker(worker: Option[SierraBibsToSnsWorkerService]) = {
    eventually {
      worker.fold(true)(_.cancelRun()) shouldBe true
    }
  }
}
