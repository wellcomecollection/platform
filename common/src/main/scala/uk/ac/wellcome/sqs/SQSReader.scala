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

import scala.collection.JavaConversions._
import scala.concurrent.{Future, blocking}

case class SQSReaderGracefulException(e: Exception)
    extends Exception(e.getMessage)

class SQSReader @Inject()(sqsClient: AmazonSQS, sqsConfig: SQSConfig)
    extends Logging {
  // SQS is not a FIFO queue so the order of arrival of messages does not necessarily reflect the order messages were sent in.
  // After a consumer reads a message from an SQS queue, AWS doesn’t delete the message immediately.
  // It "hides" the message for a fixed period, until either
  //  * the consumer tells SQS to delete the message, or
  //  * the timeout expires
  // If the timeout expires before the consumer sends a delete request, the message is unhidden and can be read by another consumer.

  def retrieveAndDeleteMessages(
    process: Message => Future[Unit]): Future[Unit] =
    Future {
      blocking {
        debug(s"Looking for new messages at ${sqsConfig.queueUrl}")
        receiveMessages()
      }
    } flatMap { messages =>
      if (messages.nonEmpty)
        info(s"Received messages $messages from queue ${sqsConfig.queueUrl}")
      else
        debug(s"Received messages $messages from queue ${sqsConfig.queueUrl}")
      processAndDeleteMessages(messages, process).map { _ =>
        ()
      }
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

  private def processAndDeleteMessages(messages: List[Message],
                                       process: Message => Future[Unit]) =
    Future.sequence(messages.map { message =>
      Future
        .successful(())
        .flatMap(_ => {
          info(s"Processing message ${message.getMessageId}")
          process(message)
        })
        .recover {
          case e: SQSReaderGracefulException =>
            info(
              s"Recoverable error processing message ${message.getMessageId}",
              e)
            throw e
          case e: Throwable =>
            error(
              s"Unrecoverable error processing message ${message.getMessageId}",
              e)
            throw e
        }
        .flatMap(_ => deleteMessage(message))
    })

  private def deleteMessage(message: Message) =
    Future {
      blocking {
        sqsClient.deleteMessage(
          new DeleteMessageRequest(sqsConfig.queueUrl,
                                   message.getReceiptHandle)
        )
        info(s"Deleted message ${message.getMessageId}")
      }
    }.recover {
      case e: Throwable =>
        error(s"Failed deleting message ${message.getMessageId}", e)
        throw e
    }
}
