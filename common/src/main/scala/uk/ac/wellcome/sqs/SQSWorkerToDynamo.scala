package uk.ac.wellcome.sqs

import akka.actor.ActorSystem
import com.amazonaws.services.dynamodbv2.model.ConditionalCheckFailedException
import com.twitter.inject.Logging
import uk.ac.wellcome.metrics.MetricsSender
import uk.ac.wellcome.models.aws.SQSMessage

import uk.ac.wellcome.utils.GlobalExecutionContext.context


import scala.concurrent.Future

abstract class SQSWorkerToDynamo[T](
    sqsReader: SQSReader,
    actorSystem: ActorSystem,
    metricsSender: MetricsSender
  ) extends SQSWorker(sqsReader, actorSystem, metricsSender) {

  private def failGracefully(message: SQSMessage, e: Throwable): Future[Unit] = {
    logger.warn(s"Failed processing $message", e)
    Future.failed(SQSReaderGracefulException(e))
  }

  def conversion(s: String): Either[Throwable, T]
  def process(t: T): Future[Unit]

  override def processMessage(message: SQSMessage): Future[Unit] =
    convertAndProcess(message)

  def convertAndProcess(message: SQSMessage): Future[Unit] = {
    conversion(message.body) match {
      case Right(t) => process(t).recover {
        case e: ConditionalCheckFailedException =>
          failGracefully(message, e)
      }
      case Left(e) => failGracefully(message, e)
    }
  }
}