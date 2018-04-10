package uk.ac.wellcome.test.fixtures

import com.amazonaws.auth._
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration
import com.amazonaws.services.sqs._
import com.amazonaws.services.sqs.model.PurgeQueueRequest

import scala.util.Random
import scala.collection.JavaConversions._
import uk.ac.wellcome.models.aws.SQSMessage

trait SQS extends ImplicitLogging {

  private val sqsEndpointUrl = "http://localhost:9324"

  private val accessKey = "access"
  private val secretKey = "secret"

  def sqsLocalFlags(queueUrl: String) = Map(
    "aws.sqs.endpoint" -> sqsEndpointUrl,
    "aws.sqs.accessKey" -> accessKey,
    "aws.sqs.secretKey" -> secretKey,
    "aws.region" -> "localhost",
    "aws.sqs.queue.url" -> queueUrl,
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

  def withLocalSqsQueue[R] = fixture[String, R](
    create = {
      val queueName: String = Random.alphanumeric take 10 mkString
      val url = sqsClient.createQueue(queueName).getQueueUrl

      sqsClient.setQueueAttributes(url, Map("VisibilityTimeout" -> "1"))
      url
    },
    destroy = { url: String =>
      safeCleanup(url) { url =>
        sqsClient.purgeQueue(new PurgeQueueRequest().withQueueUrl(url))
      }
      sqsClient.deleteQueue(url)
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
