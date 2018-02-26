package uk.ac.wellcome.sqs

import akka.actor.ActorSystem
import com.amazonaws.services.sqs.model.Message
import uk.ac.wellcome.utils.JsonUtil._
import uk.ac.wellcome.exceptions.GracefulFailureException
import uk.ac.wellcome.metrics.MetricsSender
import uk.ac.wellcome.models.aws.SQSMessage
import uk.ac.wellcome.utils.{JsonUtil, TryBackoff}
import uk.ac.wellcome.utils.GlobalExecutionContext.context
import scala.concurrent.Future
import scala.util.Try

abstract class SQSWorker(sqsReader: SQSReader,
                         actorSystem: ActorSystem,
                         metricsSender: MetricsSender)
    extends TryBackoff {

  lazy val workerName: String = this.getClass.getSimpleName

  def processMessage(message: SQSMessage): Future[Unit]

  def runSQSWorker(): Unit = run(() => processMessages, actorSystem)

  private def processMessages(): Future[Unit] = {
    info(s"Starting $workerName.")

    sqsReader.retrieveAndDeleteMessages { message =>
      Future
        .fromTry(extractMessage(message))
        .flatMap(m => {
          debug(s"Processing message: $m")

          metricsSender.timeAndCount(
            s"${workerName}_ProcessMessage",
            () => processMessage(m)
          )
        })
    }
  }

  private def extractMessage(sqsMessage: Message): Try[SQSMessage] =
    JsonUtil.fromJson[SQSMessage](sqsMessage.getBody)

  override def terminalFailureHook(): Unit =
    metricsSender.incrementCount(s"${workerName}_TerminalFailure")
}
