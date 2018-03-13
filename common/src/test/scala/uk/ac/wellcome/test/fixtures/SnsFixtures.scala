package uk.ac.wellcome.test.fixtures

import com.amazonaws.auth.{AWSStaticCredentialsProvider, BasicAWSCredentials}
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration
import com.amazonaws.services.sns.{AmazonSNS, AmazonSNSClientBuilder}
import com.fasterxml.jackson.databind.{DeserializationFeature, ObjectMapper}
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.module.scala.experimental.ScalaObjectMapper
import scala.util.Random


trait SnsFixtures {

  private val localSNSEndpointUrl = "http://localhost:9292"

  private val accessKey = "access"
  private val secretKey = "secret"

  val snsLocalFlags: Map[String, String] =
    Map(
      "aws.sns.endpoint" -> localSNSEndpointUrl,
      "aws.sns.accessKey" -> accessKey,
      "aws.sns.secretKey" -> secretKey,
      "aws.region" -> "localhost")

  val snsClient: AmazonSNS = AmazonSNSClientBuilder
    .standard()
    .withCredentials(new AWSStaticCredentialsProvider(
      new BasicAWSCredentials(accessKey, secretKey)))
    .withEndpointConfiguration(
      new EndpointConfiguration(localSNSEndpointUrl, "local"))
    .build()

  def withLocalSnsTopic[R](testWith: TestWith[String, R]) = {
    val topicName: String = Random.alphanumeric take 10 mkString
    val arn = snsClient.createTopic(topicName).getTopicArn

    try {
      testWith(arn)
    } finally {
      snsClient.deleteTopic(arn)
    }
  }

  def listMessagesReceivedFromSNS(topicArn: String): List[MessageInfo] = {
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

    val mapper = new ObjectMapper(new YAMLFactory()) with ScalaObjectMapper

    mapper.registerModule(DefaultScalaModule)
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

    val messages: Messages = mapper.readValue(string, classOf[Messages])

    messages.messages.filter(_.topic_arn == topicArn)
  }
}
