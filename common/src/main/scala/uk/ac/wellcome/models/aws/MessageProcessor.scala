package uk.ac.wellcome.models.aws

import com.amazonaws.services.sqs.AmazonSQS
import com.amazonaws.services.sqs.model.{DeleteMessageRequest, Message => AwsSQSMessage}
import com.twitter.inject.Logging
import uk.ac.wellcome.utils.JsonUtil

import scala.concurrent.Future
import scala.util.{Failure, Success}
import uk.ac.wellcome.utils.GlobalExecutionContext.context


trait MessageProcessor extends Logging {
  type Processor = (String) => Future[Unit]

  val client: AmazonSQS
  val queueUrl: String

  def deleteMessage(message: AwsSQSMessage): Unit =
    client.deleteMessage(
      new DeleteMessageRequest(queueUrl, message.getReceiptHandle)
    )

  def extractMessage(sqsMessage: AwsSQSMessage): Option[SQSMessage] =
    JsonUtil.fromJson[SQSMessage](sqsMessage.getBody) match {
      case Success(m) => Some(m)
      case Failure(e) => {
        error("Invalid message structure (not via SNS?)", e)
        None
      }
    }

  def getProcessor(message: SQSMessage): Option[Processor] =
    message.subject.flatMap(chooseProcessor)

  def processMessage(message: AwsSQSMessage): Future[Unit] =
    Future {

      val processOption = for {
        message <- extractMessage(message)
        processor <- getProcessor(message)
      } yield processor.apply(message.body)

      processOption.getOrElse({
        error(s"Unrecognised message subject.")
        throw new RuntimeException("Failed to process message")
      })

    }.map(_ => deleteMessage(message))

  def chooseProcessor(subject: String): Option[Processor]
}
