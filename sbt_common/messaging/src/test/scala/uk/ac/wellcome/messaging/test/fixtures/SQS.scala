package uk.ac.wellcome.messaging.test.fixtures

import akka.actor.ActorSystem
import com.amazonaws.services.sqs._
import com.amazonaws.services.sqs.model._
import io.circe.Encoder
import org.scalatest.Matchers
import uk.ac.wellcome.messaging.sns.NotificationMessage
import uk.ac.wellcome.messaging.sqs._
import uk.ac.wellcome.monitoring.MetricsSender
import uk.ac.wellcome.test.fixtures._

import scala.concurrent.duration._
import scala.collection.JavaConverters._
import scala.util.Random
import uk.ac.wellcome.utils.JsonUtil._

object SQS {

  case class Queue(url: String, arn: String) {
    override def toString = s"SQS.Queue(url = $url, name = $name)"
    def name = url.split("/").toList.last
  }
  case class QueuePair(queue: Queue, dlq: Queue)

}

trait SQS extends Matchers {

  import SQS._

  private val sqsInternalEndpointUrl = "http://sqs:9324"
  private val sqsEndpointUrl = "http://localhost:9324"

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
    "aws.sqs.region" -> regionName
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
      sqsClient.purgeQueue(new PurgeQueueRequest().withQueueUrl(queue.url))
      sqsClient.deleteQueue(queue.url)
    }
  )

  def withLocalSqsQueueAndDlq[R](testWith: TestWith[QueuePair, R]): R =
    withLocalSqsQueueAndDlqAndTimeout(1)(testWith)

  def withLocalSqsQueueAndDlqAndTimeout[R](visibilityTimeout: Int)(
    testWith: TestWith[QueuePair, R]): R =
    withLocalSqsQueue { dlq =>
      val queueName: String = Random.alphanumeric take 10 mkString
      val response = sqsClient.createQueue(new CreateQueueRequest()
        .withQueueName(queueName)
        .withAttributes(Map(
          "RedrivePolicy" -> s"""{"maxReceiveCount":"3", "deadLetterTargetArn":"${dlq.arn}"}""",
          "VisibilityTimeout" -> s"$visibilityTimeout").asJava))
      val arn = sqsClient
        .getQueueAttributes(response.getQueueUrl, List("QueueArn").asJava)
        .getAttributes
        .get("QueueArn")
      val queue = Queue(response.getQueueUrl, arn)
      testWith(QueuePair(queue, dlq))
    }

  val localStackSqsClient: AmazonSQS = SQSClientFactory.createSyncClient(
    region = "localhost",
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

  def withSQSStream[T, R](
    actorSystem: ActorSystem,
    queue: Queue,
    metricsSender: MetricsSender)(testwith: TestWith[SQSStream[T], R]) = {
    val sqsConfig = SQSConfig(
      queueUrl = queue.url,
      waitTime = 1 millisecond,
      maxMessages = 1
    )

    val stream = new SQSStream[T](
      actorSystem = actorSystem,
      sqsClient = asyncSqsClient,
      sqsConfig = sqsConfig,
      metricsSender = metricsSender)

    testwith(stream)
  }

  def createNotificationMessageWith(body: String): NotificationMessage =
    NotificationMessage(
      MessageId = Random.alphanumeric take 10 mkString,
      TopicArn = Random.alphanumeric take 10 mkString,
      Subject = Random.alphanumeric take 10 mkString,
      Message = body
    )

  def createNotificationMessageWith[T](message: T)(
    implicit encoder: Encoder[T]): NotificationMessage =
    createNotificationMessageWith(body = toJson(message).get)

  def sendNotificationToSQS(queue: Queue, body: String): SendMessageResult = {
    val message = createNotificationMessageWith(body = body)
    sendMessage(queue = queue, obj = message)
  }

  def sendNotificationToSQS[T](queue: Queue, message: T)(
    implicit encoder: Encoder[T]): SendMessageResult =
    sendNotificationToSQS(queue = queue, body = toJson(message).get)

  def sendMessage[T](queue: Queue, obj: T)(
    implicit encoder: Encoder[T]): SendMessageResult =
    sendMessage(queue = queue, body = toJson(obj).get)

  def sendMessage(queue: Queue, body: String): SendMessageResult =
    sqsClient.sendMessage(queue.url, body)

  def sendInvalidJSONto(queue: Queue): SendMessageResult =
    sendMessage(queue = queue, body = Random.alphanumeric take 50 mkString)

  def noMessagesAreWaitingIn(queue: Queue) = {
    // No messages in flight
    sqsClient
      .getQueueAttributes(
        queue.url,
        List("ApproximateNumberOfMessagesNotVisible").asJava
      )
      .getAttributes
      .get(
        "ApproximateNumberOfMessagesNotVisible"
      ) shouldBe "0"

    // No messages awaiting processing
    sqsClient
      .getQueueAttributes(
        queue.url,
        List("ApproximateNumberOfMessages").asJava
      )
      .getAttributes
      .get(
        "ApproximateNumberOfMessages"
      ) shouldBe "0"
  }

  def assertQueueEmpty(queue: Queue) = {
    waitVisibilityTimeoutExipiry()

    val messages = getMessages(queue)

    messages shouldBe empty
    noMessagesAreWaitingIn(queue)
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

  def waitVisibilityTimeoutExipiry() = {
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
