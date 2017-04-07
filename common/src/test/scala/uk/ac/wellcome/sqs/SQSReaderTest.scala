package uk.ac.wellcome.sqs

import com.amazonaws.services.sqs.AmazonSQS
import com.amazonaws.services.sqs.model.{Message, ReceiveMessageRequest, ReceiveMessageResult}
import org.mockito.Matchers.any
import org.mockito.Mockito
import org.mockito.Mockito.{verify, when}
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.{FunSpec, Matchers}
import org.scalatest.mockito.MockitoSugar
import uk.ac.wellcome.models.aws.SQSConfig

import scala.collection.JavaConversions._
import scala.concurrent.duration._

class SQSReaderTest extends FunSpec with MockitoSugar with Matchers with ScalaFutures with IntegrationPatience{

  it("should send a request to the sqs client with the number messages and the waiting time, and return the messages returned by the client"){
    val sqsClient = mock[AmazonSQS]
    val sqsConfig = SQSConfig("eu-west-1", "blah")
    mockSqsClient(sqsClient, sqsConfig, """["somejson"]""")
    val sqsReader = new SQSReader(sqsClient, sqsConfig, waitTime = 20 seconds, 1)

    val futureMessages = sqsReader.retrieveMessage()

    whenReady(futureMessages) {messages =>
      verifyMockCalled(sqsClient, sqsConfig)
      messages should have size 1
      messages.head.getBody should be("""["somejson"]""")
    }
  }

  it("should return a failed future if reading from the sns client fails"){
    val sqsClient = mock[AmazonSQS]
    val sqsConfig = SQSConfig("eu-west-1", "blah")
    when(sqsClient.receiveMessage(any[ReceiveMessageRequest]())).thenThrow(new RuntimeException("failed to connect to the queue"))
    val sqsReader = new SQSReader(sqsClient, sqsConfig, waitTime = 20 seconds, 1)

    val futureMessages = sqsReader.retrieveMessage()

    whenReady(futureMessages.failed) {exception =>
      exception.getMessage should be ("failed to connect to the queue")
    }
  }

  private def verifyMockCalled(sqsClient: AmazonSQS, sqsConfig: SQSConfig) = {
    verify(sqsClient).receiveMessage(new ReceiveMessageRequest(sqsConfig.queueUrl).withWaitTimeSeconds(20).withMaxNumberOfMessages(1))
  }

  private def mockSqsClient(sqsClient: AmazonSQS, sqsConfig: SQSConfig, jsonMessages: String*): Any = {
    when(sqsClient.receiveMessage(
      new ReceiveMessageRequest(sqsConfig.queueUrl).withWaitTimeSeconds(20).withMaxNumberOfMessages(1)))
      .thenReturn(createMessageResult(jsonMessages: _*))
  }

  private def createMessageResult(jsonMessages: String*) = {
    new ReceiveMessageResult().withMessages(jsonMessages.toList.map { jsonMessage =>
      createMessage(jsonMessage)
    })
  }

  private def createMessage(jsonMessage: String) = new Message().withBody(jsonMessage)
}
