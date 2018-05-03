package uk.ac.wellcome.messaging.test.fixtures

import com.amazonaws.services.sns._
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration
import com.amazonaws.auth.AWSStaticCredentialsProvider
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.module.scala.experimental.ScalaObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.annotation.JsonProperty
import uk.ac.wellcome.test.fixtures._

import scala.util.Random

object SNS {

  class Topic(val arn: String) extends AnyVal {
    override def toString = s"SNS.Topic($arn)"
  }

  object Topic {
    def apply(arn: String): Topic = new Topic(arn)
  }

}

trait SNS extends ImplicitLogging {

  import SNS._

  protected val snsInternalEndpointUrl = "http://sns:9292"
  protected val localSNSEndpointUrl = "http://localhost:9292"

  private val accessKey = "access"
  private val secretKey = "secret"

  def snsLocalFlags(topic: Topic) = snsLocalClientFlags ++ Map(
    "aws.sns.topic.arn" -> topic.arn
  )

  def snsLocalClientFlags = Map(
    "aws.sns.endpoint" -> localSNSEndpointUrl,
    "aws.sns.accessKey" -> accessKey,
    "aws.sns.secretKey" -> secretKey,
    "aws.region" -> "localhost"
  )

  private val credentials = new AWSStaticCredentialsProvider(
    new BasicAWSCredentials(accessKey, secretKey))

  val snsClient: AmazonSNS = AmazonSNSClientBuilder
    .standard()
    .withCredentials(credentials)
    .withEndpointConfiguration(
      new EndpointConfiguration(localSNSEndpointUrl, "local"))
    .build()

  def withLocalSnsTopic[R] = fixture[Topic, R](
    create = {
      val topicName = Random.alphanumeric take 10 mkString
      val arn = snsClient.createTopic(topicName).getTopicArn
      Topic(arn)
    },
    destroy = { topic =>
      snsClient.deleteTopic(topic.arn)
    }
  )

  val localStackSnsClient: AmazonSNS = AmazonSNSClientBuilder
    .standard()
    .withCredentials(credentials)
    .withEndpointConfiguration(
      new EndpointConfiguration("http://localhost:4575", "eu-west-2"))
    .build()

  def withLocalStackSnsTopic[R] = fixture[Topic, R](
    create = {
      val topicName = Random.alphanumeric take 10 mkString
      val arn = localStackSnsClient.createTopic(topicName).getTopicArn
      Topic(arn)
    },
    destroy = { topic =>
      localStackSnsClient.deleteTopic(topic.arn)
    }
  )

  private val mapper =
    (new ObjectMapper(new YAMLFactory()) with ScalaObjectMapper)
      .registerModule(DefaultScalaModule)
      .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

  def listMessagesReceivedFromSNS(topic: Topic): List[MessageInfo] = {
    /*
    This is a sample returned by the fake-sns implementation:
    ---
    topics:
    - arn: arn:aws:sns:us-east-1:123456789012:es_ingest
      name: es_ingest
    - arn: arn:aws:sns:us-east-1:123456789012:id_minter
      name: id_minter
    messages:
    - :id: acbca1e1-e3c5-4c74-86af-06a9418e8fe4
      :subject: Foo
  :message: '{"identifiers":[{"source":"Miro","sourceId":"MiroID","value":"1234"}],"title":"some
    image title","accessStatus":null}'
  :topic_arn: arn:aws:sns:us-east-1:123456789012:id_minter
  :structure:
  :target_arn:
  :received_at: 2017-04-18 13:20:45.289912607 +00:00
     */

    val string = scala.io.Source.fromURL(localSNSEndpointUrl).mkString
    val messages = mapper.readValue(string, classOf[Messages])
    messages.messages.filter(_.topic_arn == topic.arn)
  }
}

case class Messages(topics: List[TopicInfo], messages: List[MessageInfo])
case class TopicInfo(arn: String, name: String)
case class MessageInfo(@JsonProperty(":id") messageId: String,
                       @JsonProperty(":message") message: String,
                       @JsonProperty(":subject") subject: String,
                       @JsonProperty(":topic_arn") topic_arn: String)
