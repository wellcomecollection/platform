package uk.ac.wellcome.test.utils

import com.amazonaws.auth.{AWSStaticCredentialsProvider, BasicAWSCredentials}
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration
import com.amazonaws.services.sns.{AmazonSNS, AmazonSNSClientBuilder}
import org.apache.http.client.methods.HttpDelete
import org.apache.http.impl.client.DefaultHttpClient
import org.scalatest.{BeforeAndAfterEach, Suite}

trait SNSLocal extends Suite with BeforeAndAfterEach {
  val localSNSEndpointUrl = "http://localhost:9292"
  val amazonSNS: AmazonSNS = AmazonSNSClientBuilder
    .standard().withCredentials(new AWSStaticCredentialsProvider(
    new BasicAWSCredentials("access", "secret")))
    .withEndpointConfiguration(
      new EndpointConfiguration(localSNSEndpointUrl, "local"))
    .build()

  private val topicName = "es_ingest"

  //we use this implementation of SNS running in a docker container https://github.com/elruwen/fake_sns
  //Topic arns are always built in this way by this implementatiom
  val ingestTopicArn = s"arn:aws:sns:us-east-1:123456789012:$topicName"

  override def beforeEach(): Unit = {
    super.beforeEach()
    new DefaultHttpClient().execute(new HttpDelete(localSNSEndpointUrl))
    amazonSNS.createTopic(topicName)
  }

  def listMessagesReceivedFromSNS(): List[MessageInfo] = {
    /*
    This is a sample returned by the fake-sns implementation:
    ---
    topics:
    - arn: arn:aws:sns:us-east-1:123456789012:es_ingest
      name: es_ingest
    subscriptions: []
    messages:
    - :id: 1a9f3f37-54be-4a1e-9798-7fb928caad87
      :subject: subject
      :message: '"some" "Message"'
      :topic_arn: arn:aws:sns:us-east-1:123456789012:es_ingest
      :structure:
      :target_arn:
      :received_at: 2017-04-10 16:31:38.015426145 +00:00

     */

    val string = scala.io.Source.fromURL(localSNSEndpointUrl).mkString
    string
      .substring(string.indexOf("- :id:")+1).split("\n- ")
      .map { messageDetails =>
        val messageLines = messageDetails.split('\n')
        MessageInfo(
          getMessageLine(messageLines, ":id: "),
          getMessageLine(messageLines, ":message: ").replace("'", ""),
          getMessageLine(messageLines, ":subject: "))
      }
      .toList
  }

  private def getMessageLine(messageLines: Array[String],
                             fieldName: String): String = {
    messageLines
      .filter(_.contains(fieldName))
      .map {
        _.replace(fieldName, "").trim
      }
      .head
  }
}

case class MessageInfo(messageId: String, message: String, subject: String)
