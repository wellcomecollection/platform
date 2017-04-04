package uk.ac.wellcome.platform.idminter.modules

import com.amazonaws.services.sqs.AmazonSQS
import uk.ac.wellcome.models.aws.SQSConfig
import com.amazonaws.services.sqs.model.{Message, ReceiveMessageRequest}
import com.twitter.inject.Logging

import scala.collection.JavaConversions._
import scala.concurrent.duration.Duration

class SQSReader(sqsClient:AmazonSQS, sqsConfig: SQSConfig, waitTime: Duration, maxMessages: Int) extends Logging {

  def retrieveMessages(): Seq[Message] = {
    info("Polling for new messages ...")
    sqsClient.receiveMessage(
      new ReceiveMessageRequest(sqsConfig.queueUrl)
        .withWaitTimeSeconds(waitTime.toSeconds.toInt)
        .withMaxNumberOfMessages(maxMessages))
      .getMessages
  }

}
