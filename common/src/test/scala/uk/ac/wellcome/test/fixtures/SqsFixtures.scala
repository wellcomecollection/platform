package uk.ac.wellcome.test.fixtures

import com.amazonaws.auth._
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration
import com.amazonaws.services.sns.{AmazonSNS, AmazonSNSClientBuilder}
import com.amazonaws.services.sqs._
import com.amazonaws.services.sqs.model.PurgeQueueRequest
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.{DeserializationFeature, ObjectMapper}
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.module.scala.experimental.ScalaObjectMapper

import scala.util.Random
import uk.ac.wellcome.models.aws.SQSMessage

import scala.util.Try

trait SqsFixtures {

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

  val sqsClient: AmazonSQS = AmazonSQSClientBuilder
    .standard()
    .withCredentials(new AWSStaticCredentialsProvider(
      new BasicAWSCredentials(accessKey, secretKey)))
    .withEndpointConfiguration(
      new EndpointConfiguration(sqsEndpointUrl, "localhost"))
    .build()

  def withLocalSqsQueue[R](testWith: TestWith[String, R]) = {
    val queueName: String = Random.alphanumeric take 10 mkString
    val url = sqsClient.createQueue(queueName).getQueueUrl

    sqsClient.setQueueAttributes(url, Map("VisibilityTimeout" -> "1"))

    try {
      testWith(url)
    } finally {
      Try { sqsClient.purgeQueue(new PurgeQueueRequest().withQueueUrl(url)) }
      Try { sqsClient.deleteQueue(url) }
    }
  }

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

case class Messages(topics: List[TopicInfo], messages: List[MessageInfo])
case class TopicInfo(arn: String, name: String)
case class MessageInfo(@JsonProperty(":id") messageId: String,
                       @JsonProperty(":message") message: String,
                       @JsonProperty(":subject") subject: String,
                       @JsonProperty(":topic_arn") topic_arn: String)
