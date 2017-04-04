package uk.ac.wellcome.platform.idminter.modules

import com.amazonaws.services.sqs.AmazonSQS
import com.amazonaws.services.sqs.model.{Message, ReceiveMessageRequest, ReceiveMessageResult}
import org.mockito.Mockito.when
import org.mockito.Matchers.any
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.aws.SQSConfig

import scala.collection.JavaConversions._
import scala.concurrent.duration._

class SQSReaderTest extends FunSpec with MockitoSugar with Matchers with ScalaFutures with IntegrationPatience{

  it("should retrieve a message from the sqs client"){
    val sqsClient = mock[AmazonSQS]
    val sqsConfig = SQSConfig("eu-west-1", "blah")
    mockSqsClient(sqsClient, sqsConfig, """["somejson"]""")
    val sqsReader = new SQSReader(sqsClient, sqsConfig, waitTime = 20 seconds)

    val futureMessages = sqsReader.retrieveMessage()

    whenReady(futureMessages) {messages =>
      messages.isDefined should be (true)
      messages.get.getBody should be("""["somejson"]""")
    }
  }

  it("should return a failed future if reading from the sns client fails"){
    val sqsClient = mock[AmazonSQS]
    val sqsConfig = SQSConfig("eu-west-1", "blah")
    when(sqsClient.receiveMessage(any[ReceiveMessageRequest]())).thenThrow(new RuntimeException("failed to connect to the queue"))
    val sqsReader = new SQSReader(sqsClient, sqsConfig, waitTime = 20 seconds)

    val futureMessages = sqsReader.retrieveMessage()

    whenReady(futureMessages.failed) {exception =>
      exception.getMessage should be ("failed to connect to the queue")
    }
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
