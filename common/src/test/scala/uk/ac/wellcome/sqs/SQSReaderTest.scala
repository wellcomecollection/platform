package uk.ac.wellcome.sqs

import com.amazonaws.services.sqs.model.{Message, ReceiveMessageResult}
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.aws.SQSConfig
import uk.ac.wellcome.test.utils.SQSLocal

import scala.collection.JavaConversions._
import scala.concurrent.duration._

class SQSReaderTest
    extends FunSpec
    with Matchers
    with ScalaFutures
    with IntegrationPatience
    with SQSLocal {

  it("should get messages from the SQS queue, limited by the maximum number of messages and return them") {
    val sqsConfig =
      SQSConfig("eu-west-1", idMinterQueueUrl, waitTime = 20 seconds, maxMessages = 2)
    val messageStrings = List("someMessage1", "someMessage2", "someMessage3")
    messageStrings.foreach(sqsClient.sendMessage(idMinterQueueUrl, _))
    val sqsReader =
      new SQSReader(sqsClient, sqsConfig)

    val futureMessages = sqsReader.retrieveMessages()

    whenReady(futureMessages) { messages =>
      // SQS is not a FIFO queue and it only guarantees that a message is sent at least once,
      // not that it is received exactly once
      messages should have size 2
      messages.foreach { message =>
        messageStrings should contain(message.getBody)
      }
    }
  }

  it("should return a failed future if writing to the SNS topic fails") {
    val sqsConfig =
      SQSConfig("eu-west-1", "not a valid queue url", waitTime = 20 seconds, maxMessages = 1)
    val sqsReader = new SQSReader(sqsClient, sqsConfig)

    val futureMessages = sqsReader.retrieveMessages()

    whenReady(futureMessages.failed) { exception =>
      exception.getMessage should not be (empty)
    }
  }

  private def createMessage(jsonMessage: String) =
    new Message().withBody(jsonMessage)
}
