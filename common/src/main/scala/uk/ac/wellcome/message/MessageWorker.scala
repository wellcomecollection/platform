package uk.ac.wellcome.message

import akka.actor.ActorSystem
import uk.ac.wellcome.utils.JsonUtil._
import uk.ac.wellcome.metrics.MetricsSender
import uk.ac.wellcome.models.aws.SQSMessage
import uk.ac.wellcome.utils.JsonUtil.fromJson
import uk.ac.wellcome.utils.GlobalExecutionContext.context

import scala.concurrent.Future
import com.twitter.inject.Logging

import scala.concurrent.duration._
import com.amazonaws.services.s3.AmazonS3
import io.circe.Decoder
import uk.ac.wellcome.s3.S3ObjectLocation
import uk.ac.wellcome.s3.S3ObjectStore
import uk.ac.wellcome.sqs.{SQSReader, SQSWorker}
import uk.ac.wellcome.utils.JsonUtil._
import uk.ac.wellcome.sns.NotificationMessage

abstract class MessageWorker[T](sqsReader: SQSReader,
                                messageReader: MessageReader[T],
                                actorSystem: ActorSystem,
                                metricsSender: MetricsSender)
    extends Logging {

  info(s"Starting message worker=[$workerName]")

  lazy val poll = 1 second

  implicit val decoder: Decoder[T]

  private lazy val workerName: String = this.getClass.getSimpleName
  private lazy val scheduler = actorSystem.scheduler
  private val actor = scheduler.schedule(0 seconds, poll)(processMessages())

  def processMessage(message: T): Future[Unit]

  private def processMessages()(
    implicit decoderN: Decoder[NotificationMessage]): Future[Unit] = {
    sqsReader.retrieveAndDeleteMessages { message =>
      for {
        message <- messageReader.process(message)
        _ <- Future.successful { debug(s"Processing message: $message") }
        metricName = s"${workerName}_ProcessMessage"
        _ <- metricsSender.timeAndCount(
          metricName,
          () => processMessage(message))
      } yield ()
    } recover {
      case exception: Throwable => {
        logger.error(s"Failure while processing message.", exception)
        metricsSender.incrementCount(s"${workerName}_MessageProcessingFailure")
      }
    }
  }

  def stop(): Boolean = actor.cancel()
}
