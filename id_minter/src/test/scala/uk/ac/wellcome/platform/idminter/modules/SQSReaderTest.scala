package uk.ac.wellcome.platform.idminter.modules

import com.amazonaws.services.sqs.AmazonSQS
import com.amazonaws.services.sqs.model.{Message, ReceiveMessageRequest, ReceiveMessageResult}
import org.mockito.Mockito
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.aws.SQSConfig
import scala.concurrent.duration._
import scala.collection.JavaConversions._

class SQSReaderTest extends FunSpec with MockitoSugar with Matchers{

  it("should retrieve messages from the sqs client"){
    val sqsClient = mock[AmazonSQS]
    val sqsConfig = SQSConfig("eu-west-1", "blah")
    mockSqsClient(sqsClient, sqsConfig, """["somejson"]""", """["someotherjson"]""")
    val sqsReader = new SQSReader(sqsClient, sqsConfig, waitTime = 20 seconds, maxMessages = 2)

    val messages = sqsReader.retrieveMessages()

    messages should have size (2)
    messages.head.getBody should be ("""["somejson"]""")
    messages.tail.head.getBody should be ("""["someotherjson"]""")
  }

  private def mockSqsClient(sqsClient: AmazonSQS, sqsConfig: SQSConfig, jsonMessages: String*): Any = {
    Mockito.when(sqsClient.receiveMessage(
      new ReceiveMessageRequest(sqsConfig.queueUrl).withWaitTimeSeconds(20).withMaxNumberOfMessages(2)))
      .thenReturn(createMessageResult(jsonMessages: _*))
  }

  private def createMessageResult(jsonMessages: String*) = {
    new ReceiveMessageResult().withMessages(jsonMessages.toList.map { jsonMessage =>
      createMessage(jsonMessage)
    })
  }

  private def createMessage(jsonMessage: String) = new Message().withBody(jsonMessage)
}
