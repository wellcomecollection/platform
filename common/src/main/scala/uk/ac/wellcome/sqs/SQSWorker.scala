package uk.ac.wellcome.sqs

import akka.actor.ActorSystem
import com.amazonaws.services.sqs.model.Message
import com.fasterxml.jackson.core.JsonParseException
import uk.ac.wellcome.metrics.MetricsSender
import uk.ac.wellcome.models.aws.SQSMessage
import uk.ac.wellcome.utils.GlobalExecutionContext.context
import uk.ac.wellcome.utils.{JsonUtil, TryBackoff}

import scala.concurrent.Future
import scala.util.Try

abstract class SQSWorker(sqsReader: SQSReader,
                         actorSystem: ActorSystem,
                         metricsSender: MetricsSender)
    extends TryBackoff {

  val workerName: String = this.getClass.getSimpleName

  def processMessage(message: SQSMessage): Future[Unit]

  def runSQSWorker(): Unit = run(() => processMessages, actorSystem)

  private def processMessages(): Future[Unit] = {
    sqsReader.retrieveAndDeleteMessages { message =>
      Future
        .fromTry(extractMessage(message))
        .flatMap(m => processMessage(m))
    }
  }

  private def extractMessage(sqsMessage: Message): Try[SQSMessage] =
    JsonUtil.fromJson[SQSMessage](sqsMessage.getBody).recover {
      case e: Exception =>
        warn("Invalid message structure (not via SNS?)", e)
        throw SQSReaderGracefulException(e)
    }

  override def terminalFailureHook(): Unit =
    metricsSender.incrementCount(s"${workerName}_TerminalFailure")
}
