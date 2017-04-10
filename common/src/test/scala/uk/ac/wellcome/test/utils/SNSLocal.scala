package uk.ac.wellcome.test.utils

import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration
import com.amazonaws.services.sns.{AmazonSNS, AmazonSNSClientBuilder}
import org.apache.http.client.methods.HttpDelete
import org.apache.http.impl.client.DefaultHttpClient
import org.scalatest.{BeforeAndAfterEach, Suite}

trait SNSLocal extends Suite with BeforeAndAfterEach {
  val localSNSEnpointUrl = "http://localhost:9292"
  val amazonSNS: AmazonSNS = AmazonSNSClientBuilder
    .standard()
    .withEndpointConfiguration(
      new EndpointConfiguration(localSNSEnpointUrl, "local"))
    .build()
  private var topic = amazonSNS.createTopic("es_ingest")
  def ingestTopicArn: String = topic.getTopicArn

  override def beforeEach(): Unit = {
    super.beforeEach()
    new DefaultHttpClient().execute(new HttpDelete(localSNSEnpointUrl))
    topic = amazonSNS.createTopic("es_ingest")
  }

  def listMessagesReceivedFromSNS(): List[MessageInfo] = {
    val string = scala.io.Source.fromURL(localSNSEnpointUrl).mkString
    string
      .split("messages:\n-")
      .tail
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
