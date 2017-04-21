package uk.ac.wellcome.sqs

import com.amazonaws.services.sqs.AmazonSQS
import com.amazonaws.services.sqs.model.{
  DeleteMessageRequest,
  Message,
  ReceiveMessageRequest
}
import com.google.inject.Inject
import com.twitter.inject.Logging
import uk.ac.wellcome.models.aws.SQSConfig
import uk.ac.wellcome.utils.GlobalExecutionContext.context

import scala.concurrent.blocking
import scala.collection.JavaConversions._
import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

class SQSReader @Inject()(sqsClient: AmazonSQS, sqsConfig: SQSConfig)
    extends Logging {

  def retrieveAndProcessMessages[T](process: Message => T): Future[List[T]] =
    Future {
      blocking {
        debug(s"Looking for new messages at ${sqsConfig.queueUrl}")
        receiveMessages()
      }
    } flatMap { messages =>
      info(s"Received messages $messages from queue ${sqsConfig.queueUrl}")
      processAndDeleteMessages(messages, process)
    } recover {
      case exception: Throwable =>
        error(s"Error retrieving messages from queue ${sqsConfig.queueUrl}",
              exception)
        throw exception
    }

  private def receiveMessages() = {
    sqsClient
      .receiveMessage(
        new ReceiveMessageRequest(sqsConfig.queueUrl)
          .withWaitTimeSeconds(sqsConfig.waitTime.toSeconds.toInt)
          .withMaxNumberOfMessages(sqsConfig.maxMessages))
      .getMessages
      .toList
  }

  private def processAndDeleteMessages[T](
    messages: List[Message],
    process: Message => T): Future[List[T]] = {
    val triedProcessedMessages = Try {
      messages.map { message =>
        process(message)
      }
    }

    triedProcessedMessages match {
      case Success(processedMessages) =>
        Future.sequence(messages.map(deleteMessage)).map { _ =>
          processedMessages
        }
      case Failure(e) =>
        error(s"Error processing messages $messages", e)
        throw e
    }
  }

  private def deleteMessage(message: Message): Future[Unit] =
    Future {
      blocking {
        sqsClient.deleteMessage(
          new DeleteMessageRequest(sqsConfig.queueUrl,
                                   message.getReceiptHandle)
        )
      }
    }
}
