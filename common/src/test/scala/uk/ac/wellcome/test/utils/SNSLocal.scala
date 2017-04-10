package uk.ac.wellcome.test.utils

import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration
import com.amazonaws.services.sns.{AmazonSNS, AmazonSNSClientBuilder}
import org.apache.http.client.methods.HttpDelete
import org.apache.http.impl.client.DefaultHttpClient
import org.scalatest.{BeforeAndAfterEach, Suite}

trait SNSLocal extends Suite with BeforeAndAfterEach {
  val localSNSEndpointUrl = "http://localhost:9292"
  val amazonSNS: AmazonSNS = AmazonSNSClientBuilder
    .standard()
    .withEndpointConfiguration(
      new EndpointConfiguration(localSNSEndpointUrl, "local"))
    .build()
  private var topic = amazonSNS.createTopic("es_ingest")
  def ingestTopicArn: String = topic.getTopicArn

  override def beforeEach(): Unit = {
    super.beforeEach()
    new DefaultHttpClient().execute(new HttpDelete(localSNSEndpointUrl))
    topic = amazonSNS.createTopic("es_ingest")
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
