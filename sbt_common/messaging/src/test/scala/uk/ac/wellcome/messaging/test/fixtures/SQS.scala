package uk.ac.wellcome.messaging.test.fixtures

import com.amazonaws.services.sqs._
import com.amazonaws.services.sqs.model._
import org.scalatest.Matchers
import uk.ac.wellcome.messaging.sqs._
import uk.ac.wellcome.models.aws.AWSConfig
import uk.ac.wellcome.test.fixtures._

import scala.collection.JavaConverters._
import scala.util.Random

object SQS {

  case class Queue(url: String, arn: String) {
    override def toString = s"SQS.Queue(url = $url, name = $name)"
    def name = url.split("/").toList.last
  }
  case class QueuePair(queue: Queue, dlq: Queue)

}

trait SQS extends Matchers {

  import SQS._

  protected val sqsInternalEndpointUrl = "http://sqs:9324"
  protected val sqsEndpointUrl = "http://localhost:9324"

  private val regionName = "localhost"

  private val accessKey = "access"
  private val secretKey = "secret"

  def endpoint(queue: Queue) =
    s"aws-sqs://${queue.name}?amazonSQSEndpoint=$sqsInternalEndpointUrl&accessKey=&secretKey="

  def localStackEndpoint(queue: Queue) =
    s"sqs://${queue.name}"

  def sqsLocalFlags(queue: Queue) = sqsLocalClientFlags ++ Map(
    "aws.sqs.queue.url" -> queue.url,
    "aws.sqs.waitTime" -> "1"
  )

  def sqsLocalClientFlags = Map(
    "aws.sqs.endpoint" -> sqsEndpointUrl,
    "aws.sqs.accessKey" -> accessKey,
    "aws.sqs.secretKey" -> secretKey,
    "aws.region" -> regionName
  )

  val sqsClient: AmazonSQS = SQSClientFactory.createSyncClient(
    region = regionName,
    endpoint = sqsEndpointUrl,
    accessKey = accessKey,
    secretKey = secretKey
  )

  val asyncSqsClient: AmazonSQSAsync = SQSClientFactory.createAsyncClient(
    region = regionName,
    endpoint = sqsEndpointUrl,
    accessKey = accessKey,
    secretKey = secretKey
  )

  def withLocalSqsQueue[R] = fixture[Queue, R](
    create = {
      val queueName: String = Random.alphanumeric take 10 mkString
      val response = sqsClient.createQueue(queueName)
      val arn = sqsClient
        .getQueueAttributes(response.getQueueUrl, List("QueueArn").asJava)
        .getAttributes
        .get("QueueArn")
      val queue = Queue(response.getQueueUrl, arn)
      sqsClient
        .setQueueAttributes(queue.url, Map("VisibilityTimeout" -> "1").asJava)
      queue
    },
    destroy = { queue =>
      safeCleanup(queue) { url =>
        sqsClient.purgeQueue(new PurgeQueueRequest().withQueueUrl(queue.url))
      }
      sqsClient.deleteQueue(queue.url)
    }
  )

  def withLocalSqsQueueAndDlq[R](testWith: TestWith[QueuePair, R]) =
    withLocalSqsQueue { dlq =>
      val queueName: String = Random.alphanumeric take 10 mkString
      val response = sqsClient.createQueue(new CreateQueueRequest()
        .withQueueName(queueName)
        .withAttributes(Map(
          "RedrivePolicy" -> s"""{"maxReceiveCount":"3", "deadLetterTargetArn":"${dlq.arn}"}""",
          "VisibilityTimeout" -> "1").asJava))
      val arn = sqsClient
        .getQueueAttributes(response.getQueueUrl, List("QueueArn").asJava)
        .getAttributes
        .get("QueueArn")
      val queue = Queue(response.getQueueUrl, arn)
      testWith(QueuePair(queue, dlq))
    }

  val localStackSqsClient: AmazonSQS = SQSClientModule.buildSQSClient(
    awsConfig = AWSConfig(region = "localhost"),
    endpoint = "http://localhost:4576",
    accessKey = accessKey,
    secretKey = secretKey
  )

  def withLocalStackSqsQueue[R] = fixture[Queue, R](
    create = {
      val queueName: String = Random.alphanumeric take 10 mkString
      val response = localStackSqsClient.createQueue(queueName)
      val arn = localStackSqsClient
        .getQueueAttributes(response.getQueueUrl, List("QueueArn").asJava)
        .getAttributes
        .get("QueueArn")
      val queue = Queue(response.getQueueUrl, arn)

      localStackSqsClient
        .setQueueAttributes(queue.url, Map("VisibilityTimeout" -> "1").asJava)
      queue
    },
    destroy = { queue =>
      safeCleanup(queue) { url =>
        localStackSqsClient.purgeQueue(
          new PurgeQueueRequest().withQueueUrl(queue.url))
      }
      localStackSqsClient.deleteQueue(queue.url)
    }
  )

  object TestSqsMessage {
    def apply(messageBody: String) =
      SQSMessage(
        subject = Some("subject"),
        messageType = "messageType",
        topic = "topic",
        body = messageBody,
        timestamp = "timestamp"
      )

    def apply() =
      SQSMessage(
        subject = Some("subject"),
        messageType = "messageType",
        topic = "topic",
        body = """{ "foo": "bar"}""",
        timestamp = "timestamp"
      )
  }

  def assertQueueEmpty(queue: Queue) = {
    waitVisibilityTimeoutExipiry()

    val messages = getMessages(queue)

    messages shouldBe empty
  }

  def assertQueueNotEmpty(queue: Queue) = {
    waitVisibilityTimeoutExipiry()

    val messages = getMessages(queue)

    messages should not be empty
  }

  def assertQueueHasSize(queue: Queue, size: Int) = {
    waitVisibilityTimeoutExipiry()

    val messages = getMessages(queue)

    messages should have size size
  }

  private def waitVisibilityTimeoutExipiry() = {
    // The visibility timeout is set to 1 second for test queues.
    // Wait for slightly longer than that to make sure that messages
    // that fail processing become visible again before asserting.
    Thread.sleep(1500)
  }

  private def getMessages(queue: Queue) = {
    val messages = sqsClient
      .receiveMessage(
        new ReceiveMessageRequest(queue.url)
          .withMaxNumberOfMessages(10)
      )
      .getMessages
      .asScala
    messages
  }

}
