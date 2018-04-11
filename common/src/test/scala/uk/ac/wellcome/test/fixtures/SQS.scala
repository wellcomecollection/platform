package uk.ac.wellcome.test.fixtures

import com.amazonaws.auth._
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration
import com.amazonaws.services.sqs._
import com.amazonaws.services.sqs.model.PurgeQueueRequest
import uk.ac.wellcome.models.aws.SQSMessage
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.auth.AWSStaticCredentialsProvider

import scala.util.Random
import scala.collection.JavaConversions._

import scala.util.Random

object SQS {

  class Queue(val url: String) extends AnyVal {
    override def toString = s"SQS.Queue($url)"
  }

  object Queue {
    def apply(url: String): Queue = new Queue(url)
  }

}

trait SQS extends ImplicitLogging {

  import SQS._

  private val sqsEndpointUrl = "http://localhost:9324"

  private val accessKey = "access"
  private val secretKey = "secret"

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
      val url = sqsClient.createQueue(queueName).getQueueUrl

      sqsClient.setQueueAttributes(url, Map("VisibilityTimeout" -> "1"))
      Queue(url)
    },
    destroy = { queue =>
      safeCleanup(queue) { url =>
        sqsClient.purgeQueue(new PurgeQueueRequest().withQueueUrl(queue.url))
      }
      sqsClient.deleteQueue(queue.url)
    }
  )

  object TestSqsMessage {
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
