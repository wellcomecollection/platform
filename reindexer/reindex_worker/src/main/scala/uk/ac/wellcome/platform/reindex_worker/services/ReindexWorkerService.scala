package uk.ac.wellcome.platform.reindex_worker.services

import akka.actor.ActorSystem
import com.google.inject.Inject
import uk.ac.wellcome.messaging.sns.NotificationMessage
import uk.ac.wellcome.messaging.sqs.SQSStream
import uk.ac.wellcome.platform.reindex_worker.GlobalExecutionContext.context
import uk.ac.wellcome.platform.reindex_worker.models.ReindexJob
import uk.ac.wellcome.utils.JsonUtil._

import scala.concurrent.Future

class ReindexWorkerService @Inject()(
  targetService: ReindexService,
  system: ActorSystem,
  sqsStream: SQSStream[NotificationMessage]
) {
  sqsStream.foreach(this.getClass.getSimpleName, processMessage)

  private def processMessage(message: NotificationMessage): Future[Unit] =
    for {
      reindexJob <- Future.fromTry(fromJson[ReindexJob](message.Message))
      _ <- targetService.sendReindexRequests(reindexJob = reindexJob)
    } yield ()

  def stop() = system.terminate()
}
