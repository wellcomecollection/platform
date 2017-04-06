package uk.ac.wellcome.platform.idminter.modules

import com.amazonaws.services.sqs.AmazonSQS
import com.amazonaws.services.sqs.model.{Message, ReceiveMessageRequest}
import com.twitter.inject.Logging
import uk.ac.wellcome.models.aws.SQSConfig

import scala.collection.JavaConversions._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration.Duration

class SQSReader(sqsClient:AmazonSQS, sqsConfig: SQSConfig, waitTime: Duration) extends Logging {

  def retrieveMessage(): Future[Option[Message]] = Future {
      info("looking for new messages ...")
      sqsClient.receiveMessage(
        new ReceiveMessageRequest(sqsConfig.queueUrl)
          .withWaitTimeSeconds(waitTime.toSeconds.toInt)
          .withMaxNumberOfMessages(1))
        .getMessages.headOption
  }

}
