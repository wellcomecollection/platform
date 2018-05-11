package uk.ac.wellcome.messaging.message

import akka.actor.ActorSystem
import com.twitter.inject.Logging
import io.circe.Decoder
import uk.ac.wellcome.messaging.sns.NotificationMessage
import uk.ac.wellcome.monitoring.MetricsSender
import uk.ac.wellcome.utils.GlobalExecutionContext.context
import uk.ac.wellcome.utils.JsonUtil._

import scala.concurrent.Future
import scala.concurrent.duration._

abstract class MessageWorker[T](messageReader: MessageReader[T],
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
    messageReader.readAndDelete { t =>
      val metricName = s"${workerName}_ProcessMessage"

      debug(s"Processing message: $t")
      metricsSender.timeAndCount(
        metricName = metricName,
        fun = () => processMessage(t)
      )

    } recover {
      case exception: Throwable => {
        logger.error(s"Failure while processing message.", exception)
        metricsSender.incrementCount(s"${workerName}_MessageProcessingFailure")
      }
    }
  }

  def stop(): Boolean = actor.cancel()
}
