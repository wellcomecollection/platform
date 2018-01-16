package uk.ac.wellcome.sqs

import com.amazonaws.services.dynamodbv2.model.ConditionalCheckFailedException
import com.twitter.inject.Logging
import uk.ac.wellcome.models.aws.SQSMessage

import scala.concurrent.Future

trait WriteSQSToDynamo extends Logging {
  private def failGracefully(message: SQSMessage, e: Throwable): Future[Unit] = {
    logger.warn(s"Failed processing $message", e)
    Future.failed(SQSReaderGracefulException(e))
  }

  def convertAndProcess[T](
    message: SQSMessage,
    conversion: (String) => Either[Throwable, T],
    process: (T) => Future[Unit]
  ): Future[Unit] = {
    conversion(message.body) match {
      case Right(t) => process(t).recover {
        case e: ConditionalCheckFailedException =>
          failGracefully(message, e)
      }
      case Left(e) => failGracefully(message, e)
    }
  }
}