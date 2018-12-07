package uk.ac.wellcome.messaging.test.fixtures

import akka.actor.ActorSystem
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.sqs._
import com.amazonaws.services.sqs.model._
import grizzled.slf4j.Logging
import io.circe.Encoder
import org.scalatest.Matchers
import uk.ac.wellcome.messaging.sns.NotificationMessage
import uk.ac.wellcome.messaging.sqs._
import uk.ac.wellcome.monitoring.MetricsSender
import uk.ac.wellcome.test.fixtures._

import scala.collection.JavaConverters._
import scala.util.Random
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.monitoring.fixtures.MetricsSenderFixture
import uk.ac.wellcome.storage.ObjectLocation
import uk.ac.wellcome.storage.fixtures.S3.Bucket
import uk.ac.wellcome.storage.vhs.HybridRecord

object SQS {

  case class Queue(url: String, arn: String) {
    override def toString = s"SQS.Queue(url = $url, name = $name)"
    def name = url.split("/").toList.last
  }
  case class QueuePair(queue: Queue, dlq: Queue)

}

trait SQS extends Matchers with Logging with MetricsSenderFixture {

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
    withLocalSqsQueueAndDlqAndTimeout(visibilityTimeout = 1)(testWith)

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

  def withSQSStream[T, R](queue: Queue, metricsSender: MetricsSender)(
    testWith: TestWith[SQSStream[T], R])(
    implicit actorSystem: ActorSystem): R = {
    val sqsConfig = createSQSConfigWith(queue)

    val stream = new SQSStream[T](
      sqsClient = asyncSqsClient,
      sqsConfig = sqsConfig,
      metricsSender = metricsSender)

    testWith(stream)
  }

  def withSQSStream[T, R](queue: Queue)(testWith: TestWith[SQSStream[T], R])(
    implicit actorSystem: ActorSystem): R =
    withMetricsSender(actorSystem) { metricsSender =>
      withSQSStream[T, R](queue, metricsSender) { sqsStream =>
        testWith(sqsStream)
      }
    }

  def createNotificationMessageWith(body: String): NotificationMessage =
    NotificationMessage(body = body)

  def createNotificationMessageWith[T](message: T)(
    implicit encoder: Encoder[T]): NotificationMessage =
    createNotificationMessageWith(body = toJson(message).get)

  def createHybridRecordWith[T](
    t: T,
    version: Int = 1,
    s3Client: AmazonS3,
    bucket: Bucket)(implicit encoder: Encoder[T]): HybridRecord = {
    val s3key = Random.alphanumeric take 10 mkString
    val content = toJson(t).get
    s3Client.putObject(bucket.name, s3key, content)

    HybridRecord(
      id = Random.alphanumeric take 10 mkString,
      version = version,
      location = ObjectLocation(namespace = bucket.name, key = s3key)
    )
  }

  /** Store an object in S3 and create the HybridRecordNotification that should be sent to SNS. */
  def createHybridRecordNotificationWith[T](
    t: T,
    version: Int = 1,
    s3Client: AmazonS3,
    bucket: Bucket)(implicit encoder: Encoder[T]): NotificationMessage = {
    val hybridRecord = createHybridRecordWith[T](
      t,
      version = version,
      s3Client = s3Client,
      bucket = bucket
    )

    createNotificationMessageWith(
      message = hybridRecord
    )
  }

  def sendNotificationToSQS(queue: Queue, body: String): SendMessageResult = {
    val message = createNotificationMessageWith(body = body)

    sendSqsMessage(queue = queue, obj = message)
  }

  def sendNotificationToSQS[T](queue: Queue, message: T)(
    implicit encoder: Encoder[T]): SendMessageResult =
    sendNotificationToSQS(queue = queue, body = toJson[T](message).get)

  def sendSqsMessage[T](queue: Queue, obj: T)(
    implicit encoder: Encoder[T]): SendMessageResult =
    sendMessageToSqsClient(queue = queue, body = toJson[T](obj).get)

  def sendInvalidJSONto(queue: Queue): SendMessageResult =
    sendMessageToSqsClient(
      queue = queue,
      body = Random.alphanumeric take 50 mkString)

  private def sendMessageToSqsClient(queue: Queue,
                                     body: String): SendMessageResult = {
    debug(s"Sending message to ${queue.url}: ${body}")

    sqsClient.sendMessage(queue.url, body)
  }

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

  def assertQueuePairSizes(queue: QueuePair, qSize: Int, dlqSize: Int) = {
    waitVisibilityTimeoutExipiry()

    val messagesDlq = getMessages(queue.dlq)
    val messagesDlqSize = messagesDlq.size

    val messagesQ = getMessages(queue.queue)
    val messagesQSize = messagesQ.size

    debug(
      s"\ndlq: ${queue.dlq.url}, ${messagesDlqSize}\nqueue: ${queue.queue.url}, ${messagesQSize}")

    debug(s"$messagesDlq")

    messagesQSize shouldBe qSize
    messagesDlqSize shouldBe dlqSize
  }

  def assertQueueHasSize(queue: Queue, size: Int) = {
    waitVisibilityTimeoutExipiry()

    val messages = getMessages(queue)
    val messagesSize = messages.size

    messagesSize shouldBe size
  }

  def waitVisibilityTimeoutExipiry() = {
    // The visibility timeout is set to 1 second for test queues.
    // Wait for slightly longer than that to make sure that messages
    // that fail processing become visible again before asserting.
    Thread.sleep(1500)
  }

  def getMessages(queue: Queue) = {
    val messages = sqsClient
      .receiveMessage(
        new ReceiveMessageRequest(queue.url)
          .withMaxNumberOfMessages(10)
      )
      .getMessages
      .asScala
    messages
  }

  def createSQSConfigWith(queue: Queue): SQSConfig =
    SQSConfig(queueUrl = queue.url)
}
