package uk.ac.wellcome.platform.sierra_to_dynamo.services

import akka.actor.ActorSystem
import com.gu.scanamo.Scanamo
import org.scalatest.{BeforeAndAfterEach, FunSpec, Matchers}
import org.scalatest.concurrent.Eventually
import org.scalatest.mockito.MockitoSugar
import uk.ac.wellcome.metrics.MetricsSender
import uk.ac.wellcome.models.aws.{SQSConfig, SQSMessage}
import uk.ac.wellcome.platform.sierra_to_dynamo.locals.SierraDynamoDBLocal
import uk.ac.wellcome.platform.sierra_to_dynamo.models.SierraRecord
import uk.ac.wellcome.platform.sierra_to_dynamo.models.SierraRecord._
import uk.ac.wellcome.sqs.SQSReader
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
    with BeforeAndAfterEach {

  val queueUrl = createQueueAndReturnUrl("sierra-test-queue")
  val mockMetrics = mock[MetricsSender]
  val worker = new SierraToDynamoWorkerService(
    new SQSReader(sqsClient, SQSConfig(queueUrl, 1.second, 1)),
    ActorSystem(),
    mockMetrics,
    dynamoDbClient,
    "http://localhost:8080",
    "key",
    "secret",
    "items",
    tableName
  )

  override def afterEach(): Unit = {
    worker.cancelRun()
    super.afterEach()
  }

  it("should read a window message from sqs, retrieve the items from sierra and insert into DynamoDb") {
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
  }
}
