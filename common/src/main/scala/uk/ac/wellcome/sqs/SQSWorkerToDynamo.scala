package uk.ac.wellcome.sqs

import akka.actor.ActorSystem
import com.amazonaws.services.dynamodbv2.model.ConditionalCheckFailedException
import com.twitter.inject.Logging
import io.circe.Decoder
import uk.ac.wellcome.exceptions.GracefulFailureException
import uk.ac.wellcome.metrics.MetricsSender
import uk.ac.wellcome.models.aws.SQSMessage
import uk.ac.wellcome.models.transformable.sierra.SierraRecord
import uk.ac.wellcome.utils.GlobalExecutionContext.context
import uk.ac.wellcome.utils.JsonUtil._

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

abstract class SQSWorkerToDynamo[T](
    sqsReader: SQSReader,
    actorSystem: ActorSystem,
    metricsSender: MetricsSender
  ) extends SQSWorker(sqsReader, actorSystem, metricsSender) {

  private def failGracefully(message: SQSMessage, e: Throwable): Future[Unit] = {
    logger.warn(s"Failed processing $message", e)
    Future.failed(GracefulFailureException(e))
  }

  def store(t: T): Future[Unit]

  implicit val decoder: Decoder[T]

  override def processMessage(message: SQSMessage): Future[Unit] =
    fromJson[T](message.body) match {
      case Success(t: T) => store(t).recover {
        case e: ConditionalCheckFailedException =>
          failGracefully(message, e)
      }
      case Failure(e: Throwable) => failGracefully(message, e)
    }
}