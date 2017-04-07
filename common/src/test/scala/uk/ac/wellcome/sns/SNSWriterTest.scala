package uk.ac.wellcome.sns

import com.amazonaws.services.sns.AmazonSNS
import com.amazonaws.services.sns.model.{PublishRequest, PublishResult}
import org.mockito.Matchers.any
import org.mockito.Mockito.{verify, when}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.PublishAttempt
import uk.ac.wellcome.models.aws.SNSConfig

class SNSWriterTest extends FunSpec with MockitoSugar with ScalaFutures with Matchers{

  val snsConfig = SNSConfig("eu-west-1", "topic")

  it("should send a message with subject to the SNS client and return a publish attempt with the id of the request"){
    val snsClient = mockAmazonSNS
    val snsWriter = new SNSWriter(snsClient, snsConfig)
    val message = "someMessage"
    val subject = "subject"
    val futurePublishAttempt = snsWriter.writeMessage(message, Some(subject))

    whenReady(futurePublishAttempt){publishAttempt =>
      verify(snsClient).publish(new PublishRequest(snsConfig.topicArn, message, subject))
      publishAttempt shouldBe PublishAttempt("1")
    }
  }

  it("should send a message with no subject to the SNS client with the default subject"){
    val snsClient = mockAmazonSNS
    val snsWriter = new SNSWriter(snsClient, snsConfig)
    val message = "someMessage"

    val futurePublishAttempt = snsWriter.writeMessage(message, None)

    whenReady(futurePublishAttempt){publishAttempt =>
      verify(snsClient).publish(new PublishRequest(snsConfig.topicArn, message, "subject-not-specified"))
      publishAttempt shouldBe PublishAttempt("1")
    }
  }

  it("should return a failed future if it fails to publish the message"){
    val snsClient = mock[AmazonSNS]
    when(snsClient.publish(any[PublishRequest])).thenThrow(new RuntimeException("failed to publish message"))
    val snsWriter = new SNSWriter(snsClient, snsConfig)

    val futurePublishAttempt = snsWriter.writeMessage("someMessage", Some("subject"))

    whenReady(futurePublishAttempt.failed){exception =>
      exception.getMessage shouldBe "failed to publish message"
    }
  }

  private def mockAmazonSNS = {
    val snsClient = mock[AmazonSNS]
    when(snsClient.publish(any[PublishRequest])).thenReturn(new PublishResult().withMessageId("1"))
    snsClient
  }
}
