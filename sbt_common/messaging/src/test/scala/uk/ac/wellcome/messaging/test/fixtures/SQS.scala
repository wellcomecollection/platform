package uk.ac.wellcome.messaging.test.fixtures

import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration
import com.amazonaws.services.sqs._
import com.amazonaws.services.sqs.model.{
  PurgeQueueRequest,
  ReceiveMessageRequest
}
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.auth.AWSStaticCredentialsProvider
import org.scalatest.Matchers
import uk.ac.wellcome.messaging.sqs.SQSMessage
import uk.ac.wellcome.test.fixtures._

import scala.collection.JavaConversions._
import scala.util.Random

object SQS {

  case class Queue(url: String, arn: String) {
    override def toString = s"SQS.Queue(url = $url, name = $name)"
    def name = url.split("/").toList.last
  }

}

trait SQS extends Matchers {

  import SQS._

  protected val sqsInternalEndpointUrl = "http://sqs:9324"
  protected val sqsEndpointUrl = "http://localhost:9324"

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
    "aws.region" -> "localhost"
  )

  private val credentials = new AWSStaticCredentialsProvider(
    new BasicAWSCredentials(accessKey, secretKey))

  val sqsClient: AmazonSQS = AmazonSQSClientBuilder
    .standard()
    .withCredentials(credentials)
    .withEndpointConfiguration(
      new EndpointConfiguration(sqsEndpointUrl, "localhost"))
    .build()

  val asyncSqsClient: AmazonSQSAsync = AmazonSQSAsyncClientBuilder
    .standard()
    .withCredentials(credentials)
    .withEndpointConfiguration(
      new EndpointConfiguration(sqsEndpointUrl, "localhost"))
    .build()

  def withLocalSqsQueue[R] = fixture[Queue, R](
    create = {
      val queueName: String = Random.alphanumeric take 10 mkString
      val response = sqsClient.createQueue(queueName)
      val arn = sqsClient
        .getQueueAttributes(response.getQueueUrl, List("QueueArn"))
        .getAttributes
        .get("QueueArn")
      val queue = Queue(response.getQueueUrl, arn)
      sqsClient.setQueueAttributes(queue.url, Map("VisibilityTimeout" -> "1"))
      queue
    },
    destroy = { queue =>
      safeCleanup(queue) { url =>
        sqsClient.purgeQueue(new PurgeQueueRequest().withQueueUrl(queue.url))
      }
      sqsClient.deleteQueue(queue.url)
    }
  )

  val localStackSqsClient: AmazonSQS = AmazonSQSClientBuilder
    .standard()
    .withCredentials(credentials)
    .withEndpointConfiguration(
      new EndpointConfiguration("http://localhost:4576", "localhost"))
    .build()

  def withLocalStackSqsQueue[R] = fixture[Queue, R](
    create = {
      val queueName: String = Random.alphanumeric take 10 mkString
      val response = localStackSqsClient.createQueue(queueName)
      val arn = localStackSqsClient
        .getQueueAttributes(response.getQueueUrl, List("QueueArn"))
        .getAttributes
        .get("QueueArn")
      val queue = Queue(response.getQueueUrl, arn)

      localStackSqsClient
        .setQueueAttributes(queue.url, Map("VisibilityTimeout" -> "1"))
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
    // The visibility timeout is set to 1 second for test queues.
    // Wait for slightly longer than that to make sure that messages
    // that fail processing become visible again before asserting.
    Thread.sleep(1500)

    val messages = sqsClient
      .receiveMessage(
        new ReceiveMessageRequest(queue.url)
          .withMaxNumberOfMessages(1)
      )
      .getMessages
      .toList

    messages shouldBe empty
  }

  def assertQueueNotEmpty(queue: Queue) = {
    // The visibility timeout is set to 1 second for test queues.
    // Wait for slightly longer than that to make sure that messages
    // that fail processing become visible again before asserting.
    Thread.sleep(1500)

    val messages = sqsClient
      .receiveMessage(
        new ReceiveMessageRequest(queue.url)
          .withMaxNumberOfMessages(1)
      )
      .getMessages
      .toList

    messages should not be empty
  }

}
