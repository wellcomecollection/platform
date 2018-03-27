package uk.ac.wellcome.sqs

import com.amazonaws.services.sqs.AmazonSQS
import com.amazonaws.services.sqs.model.{
  DeleteMessageRequest,
  Message,
  ReceiveMessageRequest
}
import com.google.inject.Inject
import com.twitter.inject.Logging
import uk.ac.wellcome.exceptions.GracefulFailureException
import uk.ac.wellcome.models.aws.SQSConfig
import uk.ac.wellcome.utils.GlobalExecutionContext.context
import scala.collection.JavaConversions._
import scala.concurrent.{blocking, Future}

class SQSReader @Inject()(sqsClient: AmazonSQS, sqsConfig: SQSConfig)
    extends Logging {
  // SQS is not a FIFO queue so the order of arrival of messages does not necessarily reflect the order messages were sent in.
  // After a consumer reads a message from an SQS queue, AWS doesn’t delete the message immediately.
  // It "hides" the message for a fixed period, until either
  //  * the consumer tells SQS to delete the message, or
  //  * the timeout expires
  // If the timeout expires before the consumer sends a delete request, the message is unhidden and can be read by another consumer.

  def retrieveAndDeleteMessages(
    process: Message => Future[Unit]): Future[Unit] = {
    val eventuallyProcessMessages =
      for {
        _ <- Future.successful {
          debug(s"Looking for new messages at ${sqsConfig.queueUrl}")
        }
        ms <- receiveMessages()
        _ <- Future.sequence { ms.map { processAndDeleteMessage(_, process) } }
      } yield {
        val messageIds = ms.map { _.getMessageId }

        if (ms.nonEmpty) {
          info(
            s"Received messages $messageIds from queue ${sqsConfig.queueUrl}")
        } else {
          debug(
            s"Received messages $messageIds from queue ${sqsConfig.queueUrl}")
        }

        ()
      }

    eventuallyProcessMessages recover {
      case exception: Throwable =>
        warn(
          s"Error processing messages from queue ${sqsConfig.queueUrl}",
          exception)
        exception.printStackTrace()
        throw exception
    }
  }

  private def receiveMessages() = Future {
    blocking {
      sqsClient
        .receiveMessage(
          new ReceiveMessageRequest(sqsConfig.queueUrl)
            .withWaitTimeSeconds(sqsConfig.waitTime.toSeconds.toInt)
            .withMaxNumberOfMessages(sqsConfig.maxMessages)
        )
        .getMessages
        .toList
    }
  }

  private def processAndDeleteMessage(message: Message,
                                      process: Message => Future[Unit]) = {
    val eventuallyProcessAndDelete =
      for {
        _ <- Future.successful {
          info(s"Processing message ${message.getMessageId}")
        }
        _ <- process(message)
        _ <- deleteMessage(message)
      } yield ()

    eventuallyProcessAndDelete recover {
      case e: GracefulFailureException =>
        warn(s"Error processing the message=[${message.getMessageId}]", e)
        ()
      case e: Throwable => throw e
    }
  }

  private def deleteMessage(message: Message) =
    Future {
      blocking {
        val request = new DeleteMessageRequest(
          sqsConfig.queueUrl,
          message.getReceiptHandle)
        sqsClient.deleteMessage(request)
        info(s"Deleted message ${message.getMessageId}")
      }
    }
}
