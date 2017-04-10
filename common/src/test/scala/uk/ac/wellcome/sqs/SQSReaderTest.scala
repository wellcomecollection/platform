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
    val sqsConfig = SQSConfig("eu-west-1", idMinterQueueUrl)
    sqsClient.sendMessage(idMinterQueueUrl, "someMessage1")
    sqsClient.sendMessage(idMinterQueueUrl, "someMessage2")
    sqsClient.sendMessage(idMinterQueueUrl, "someMessage3")
    val sqsReader =
      new SQSReader(sqsClient, sqsConfig, waitTime = 20 seconds, 2)

    val futureMessages = sqsReader.retrieveMessages()

    whenReady(futureMessages) { messages =>
      messages should have size 2
      val messageBodies = messages.map { _.getBody }
      messageBodies should contain("someMessage1")
      messageBodies should contain("someMessage2")
    }
  }

  it("should return a failed future if reading from the sns client fails") {
    val sqsConfig = SQSConfig("eu-west-1", "not a valid queue url")
    val sqsReader =
      new SQSReader(sqsClient, sqsConfig, waitTime = 20 seconds, 1)

    val futureMessages = sqsReader.retrieveMessages()

    whenReady(futureMessages.failed) { exception =>
      exception.getMessage should not be (empty)
    }
  }

  private def createMessage(jsonMessage: String) =
    new Message().withBody(jsonMessage)
}
