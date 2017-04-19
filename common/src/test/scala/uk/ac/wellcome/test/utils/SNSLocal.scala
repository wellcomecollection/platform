package uk.ac.wellcome.test.utils

import com.amazonaws.auth.{AWSStaticCredentialsProvider, BasicAWSCredentials}
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration
import com.amazonaws.services.sns.{AmazonSNS, AmazonSNSClientBuilder}
import com.twitter.inject.Logging
import org.apache.http.client.methods.HttpDelete
import org.apache.http.impl.client.DefaultHttpClient
import org.scalatest.{BeforeAndAfterEach, Suite}

trait SNSLocal extends Suite with BeforeAndAfterEach with Logging {
  private val localSNSEndpointUrl = "http://localhost:9292"
  val amazonSNS: AmazonSNS = AmazonSNSClientBuilder
    .standard()
    .withCredentials(new AWSStaticCredentialsProvider(
      new BasicAWSCredentials("access", "secret")))
    .withEndpointConfiguration(
      new EndpointConfiguration(localSNSEndpointUrl, "local"))
    .build()

  private val ingestTopicName = "es_ingest"
  private val idMinterTopicName = "id_minter"

  val ingestTopicArn = amazonSNS.createTopic(ingestTopicName).getTopicArn
  val idMinterTopicArn = amazonSNS.createTopic(idMinterTopicName).getTopicArn

  override def beforeEach(): Unit = {
    super.beforeEach()
    new DefaultHttpClient()
      .execute(new HttpDelete(s"$localSNSEndpointUrl/messages"))
  }


  //TODO this is getting a bit complicated.
  // Try using https://hub.docker.com/r/s12v/sns/ docker image instead of the current one and subscribe a sqs queue to it to check for messages.
  // Couldn't make it work but should retry without proxies in local development machine
  def listMessagesReceivedFromSNS(): List[MessageInfo] = {
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
    debug(s"""Messages received by fake-sns:
         |$string""".stripMargin)
    val indexOfFirstMessage = string.indexOf("- :id:")
    if (indexOfFirstMessage < 0) {
      Nil
    } else {
      string
        .substring(indexOfFirstMessage + "- :".size)
        .split("\n- ")
        .map { messageDetails =>
          val messageLines = messageDetails.split("\n\\s*:")
          MessageInfo(
            getMessageLine(messageLines, "id: "),
            getMessageLine(messageLines, "message: ")
              .replace("'", "")
              .replace("\n   ", ""),
            getMessageLine(messageLines, "subject: ")
          )
        }
        .toList
    }
  }

  private def getMessageLine(messageLines: Array[String],
                             fieldName: String): String = {
    messageLines
      .filter(_.contains(fieldName))
      .map {
        _.replace(fieldName, "").trim
      }
      .headOption
      .getOrElse("")
  }
}

case class MessageInfo(messageId: String, message: String, subject: String)
