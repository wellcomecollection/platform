package uk.ac.wellcome.sqs

import akka.actor.ActorSystem
import uk.ac.wellcome.utils.JsonUtil._
import uk.ac.wellcome.metrics.MetricsSender
import uk.ac.wellcome.models.aws.SQSMessage
import uk.ac.wellcome.utils.JsonUtil.fromJson
import uk.ac.wellcome.utils.GlobalExecutionContext.context
import scala.concurrent.Future
import com.twitter.inject.Logging
import scala.concurrent.duration._

abstract class SQSWorker(sqsReader: SQSReader,
                         actorSystem: ActorSystem,
                         metricsSender: MetricsSender)
    extends Logging {

  info(s"Starting SQS worker=[$workerName]")

  lazy val poll = 1 second
  private lazy val workerName: String = this.getClass.getSimpleName
  private lazy val scheduler = actorSystem.scheduler
  private val actor = scheduler.schedule(0 seconds, poll)(processMessages())

  def processMessage(message: SQSMessage): Future[Unit]

  private def processMessages(): Future[Unit] = {
    sqsReader.retrieveAndDeleteMessages { message =>
      for {
        m <- Future.fromTry { fromJson[SQSMessage](message.getBody) }
        _ <- Future.successful { debug(s"Processing message: $m") }
        metricName = s"${workerName}_ProcessMessage"
        _ <- metricsSender.timeAndCount(metricName, () => processMessage(m))
      } yield ()
    } recover {
      case _: Throwable => terminalFailureHook()
    }
  }

  def terminalFailureHook(): Unit =
    metricsSender.incrementCount(s"${workerName}_TerminalFailure")

  def stop(): Boolean = actor.cancel()

}
