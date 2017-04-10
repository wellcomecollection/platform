package uk.ac.wellcome.sns

import com.amazonaws.services.sns.AmazonSNS
import com.amazonaws.services.sns.model.PublishRequest
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.aws.SNSConfig
import uk.ac.wellcome.test.utils.SNSLocal

class SNSWriterTest extends FunSpec with MockitoSugar with ScalaFutures with Matchers with SNSLocal with IntegrationPatience{

  val snsConfig = SNSConfig("eu-west-1", ingestTopicArn)

  it("should send a message with subject to the SNS client and return a publish attempt with the id of the request"){
    val snsWriter = new SNSWriter(amazonSNS, snsConfig)
    val message = "someMessage"
    val subject = "subject"
    val futurePublishAttempt = snsWriter.writeMessage(message, Some(subject))

    whenReady(futurePublishAttempt){publishAttempt =>
      val messages = listMessagesReceivedFromSNS()
      messages should have size (1)
      messages.head.message shouldBe "someMessage"
      messages.head.subject shouldBe "subject"
      publishAttempt.id should be (messages.head.messageId)
    }
  }

  it("should send a message with no subject to the SNS client with the default subject"){
    val snsWriter = new SNSWriter(amazonSNS, snsConfig)
    val message = "someMessage"

    val futurePublishAttempt = snsWriter.writeMessage(message, None)

    whenReady(futurePublishAttempt){_ =>
      val messages = listMessagesReceivedFromSNS()
      messages should have size (1)
      messages.head.subject shouldBe "subject-not-specified"
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

}
