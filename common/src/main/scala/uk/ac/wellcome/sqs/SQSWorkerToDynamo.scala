package uk.ac.wellcome.sqs

import akka.actor.ActorSystem
import com.amazonaws.services.dynamodbv2.model.ConditionalCheckFailedException
import io.circe.Decoder
import uk.ac.wellcome.exceptions.GracefulFailureException
import uk.ac.wellcome.metrics.MetricsSender
import uk.ac.wellcome.models.aws.SQSMessage
import uk.ac.wellcome.utils.GlobalExecutionContext.context
import uk.ac.wellcome.utils.JsonUtil.fromJson
import com.amazonaws.services.s3.AmazonS3

import scala.concurrent.Future

abstract class SQSWorkerToDynamo[T](
  sqsReader: SQSReader,
  actorSystem: ActorSystem,
  metricsSender: MetricsSender
) extends SQSWorker(sqsReader, actorSystem, metricsSender) {

  implicit val decoder: Decoder[T]
  def store(t: T): Future[Unit]

  override def processMessage(message: SQSMessage): Future[Unit] =
    for {
      t <- Future.fromTry(fromJson[T](message.body))
      _ <- store(t).recover {
        case e: ConditionalCheckFailedException =>
          logger.warn(s"Processing $message failed a Conditional Update", e)
          throw GracefulFailureException(e)
      }
    } yield ()
}
