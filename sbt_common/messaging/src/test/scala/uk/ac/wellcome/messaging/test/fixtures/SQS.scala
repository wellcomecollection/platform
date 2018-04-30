package uk.ac.wellcome.messaging.test.fixtures

import com.amazonaws.auth._
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration
import com.amazonaws.services.sqs._
import com.amazonaws.services.sqs.model.PurgeQueueRequest
import uk.ac.wellcome.models.aws.SQSMessage
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.auth.AWSStaticCredentialsProvider
import uk.ac.wellcome.test.fixtures.ImplicitLogging

import scala.util.Random
import scala.collection.JavaConversions._

import scala.util.Random

object SQS {

  case class Queue(url: String, arn: String) {
    override def toString = s"SQS.Queue(url = $url, name = $name)"
    def name = url.split("/").toList.last
  }

}

trait SQS extends ImplicitLogging {

  import SQS._

  protected val sqsInternalEndpointUrl = "http://sqs:9324"
  protected val sqsEndpointUrl = "http://localhost:9324"

  private val accessKey = "access"
  private val secretKey = "secret"

  def endpoint(queue: Queue) =
    s"aws-sqs://${queue.name}?amazonSQSEndpoint=$sqsInternalEndpointUrl&accessKey=&secretKey="

  def localStackEndpoint(queue: Queue) =
    s"sqs://${queue.name}"

  def sqsLocalFlags(queue: Queue) = Map(
    "aws.sqs.endpoint" -> sqsEndpointUrl,
    "aws.sqs.accessKey" -> accessKey,
    "aws.sqs.secretKey" -> secretKey,
    "aws.region" -> "localhost",
    "aws.sqs.queue.url" -> queue.url,
    "aws.sqs.waitTime" -> "1"
  )

  private val credentials = new AWSStaticCredentialsProvider(
    new BasicAWSCredentials(accessKey, secretKey))

  val sqsClient: AmazonSQS = AmazonSQSClientBuilder
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

}
