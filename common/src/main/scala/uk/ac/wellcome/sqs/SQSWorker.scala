package uk.ac.wellcome.sqs

import akka.actor.ActorSystem
import com.amazonaws.services.sqs.model.Message
import com.twitter.inject.{Injector, TwitterModule}
import uk.ac.wellcome.models.aws.SQSMessage
import uk.ac.wellcome.utils.{JsonUtil, TryBackoff}

import scala.concurrent.Future
import uk.ac.wellcome.utils.GlobalExecutionContext.context

import scala.util.Try


trait SQSWorker extends TwitterModule with TryBackoff {
  override def singletonStartup(injector: Injector) {
    info("Starting SQS worker")

    val system = injector.instance[ActorSystem]
    val sqsReader = injector.instance[SQSReader]

    run(() => processMessages(sqsReader, injector), system)
  }

  def processMessage(message: SQSMessage, injector: Injector): Future[Unit]

  private def processMessages(
                       sqsReader: SQSReader,
                       injector: Injector
                     ): Future[Unit] = {
    sqsReader.retrieveAndDeleteMessages { message =>
      Future.fromTry(extractMessage(message))
        .flatMap(m => processMessage(m, injector))
    }
  }

  private def extractMessage(sqsMessage: Message): Try[SQSMessage] =
    JsonUtil.fromJson[SQSMessage](sqsMessage.getBody).recover{
      case e: Throwable =>
        error("Invalid message structure (not via SNS?)", e)
        throw e
    }


  override def singletonShutdown(injector: Injector) {
    info("Terminating SQS worker")
    cancelRun()
    val system = injector.instance[ActorSystem]
    system.terminate()
  }
}