package uk.ac.wellcome.platform.reindex_request_processor.services

import akka.actor.ActorSystem
import com.google.inject.Inject
import uk.ac.wellcome.messaging.sns.NotificationMessage
import uk.ac.wellcome.messaging.sqs.SQSStream
import uk.ac.wellcome.platform.reindex_request_processor.GlobalExecutionContext.context
import uk.ac.wellcome.utils.JsonUtil._

import scala.concurrent.Future
import scala.util.Try

class ReindexWorkerService @Inject()(
  system: ActorSystem,
  sqsStream: SQSStream[NotificationMessage]
) {
  sqsStream.foreach(this.getClass.getSimpleName, processMessage)

  private def processMessage(message: NotificationMessage): Future[Unit] =
    for {
      _ <- Future.fromTry(
        Try { println(message) }
      )
    } yield ()

  def stop() = system.terminate()
}
