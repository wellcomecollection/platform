package uk.ac.wellcome.platform.sierra_item_merger.services

import akka.actor.ActorSystem
import com.google.inject.Inject
import uk.ac.wellcome.messaging.sns.NotificationMessage
import uk.ac.wellcome.messaging.sqs.SQSStream
import uk.ac.wellcome.models.transformable.sierra.SierraItemRecord
import uk.ac.wellcome.storage.GlobalExecutionContext
import uk.ac.wellcome.utils.JsonUtil._

import scala.concurrent.{ExecutionContext, Future}

class SierraItemMergerWorkerService @Inject()(
  system: ActorSystem,
  sqsStream: SQSStream[NotificationMessage],
  sierraItemMergerUpdaterService: SierraItemMergerUpdaterService
) {

  implicit val ec: ExecutionContext = GlobalExecutionContext.context

  sqsStream.foreach(this.getClass.getSimpleName, process)

  private def process(message: NotificationMessage): Future[Unit] =
    for {
      record <- Future.fromTry(fromJson[SierraItemRecord](message.Message))
      _ <- sierraItemMergerUpdaterService.update(record)
    } yield ()

  def stop() = system.terminate()
}
