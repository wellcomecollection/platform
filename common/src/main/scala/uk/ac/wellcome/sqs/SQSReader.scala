package uk.ac.wellcome.sqs

import com.amazonaws.services.sqs.AmazonSQS
import com.amazonaws.services.sqs.model.{Message, ReceiveMessageRequest}
import com.google.inject.Inject
import com.twitter.inject.Logging
import uk.ac.wellcome.models.aws.SQSConfig

import scala.concurrent.Future
import scala.concurrent.duration.Duration
import scala.collection.JavaConversions._

import scala.concurrent.ExecutionContext.Implicits.global

class SQSReader @Inject()(sqsClient: AmazonSQS, sqsConfig: SQSConfig, waitTime: Duration, maxMessages: Integer) extends Logging {

  def retrieveMessages(): Future[List[Message]] = Future {
      info("looking for new messages ...")
      sqsClient.receiveMessage(
        new ReceiveMessageRequest(sqsConfig.queueUrl)
          .withWaitTimeSeconds(waitTime.toSeconds.toInt)
          .withMaxNumberOfMessages(maxMessages))
        .getMessages.toList
  }

}
