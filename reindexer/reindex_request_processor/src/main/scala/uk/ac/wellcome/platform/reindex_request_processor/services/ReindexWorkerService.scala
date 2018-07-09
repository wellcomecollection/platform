package uk.ac.wellcome.platform.reindex_request_processor.services

import akka.actor.ActorSystem
import com.google.inject.Inject
import uk.ac.wellcome.messaging.sns.NotificationMessage
import uk.ac.wellcome.messaging.sqs.SQSStream
import uk.ac.wellcome.platform.reindex_request_processor.GlobalExecutionContext.context

import scala.concurrent.Future

class ReindexWorkerService @Inject()(
  system: ActorSystem,
  sqsStream: SQSStream[NotificationMessage]
) {
  sqsStream.foreach(this.getClass.getSimpleName, processMessage)

  private def processMessage(message: NotificationMessage): Future[Unit] = Future {
    println(message)
  }

  def stop() = system.terminate()
}
