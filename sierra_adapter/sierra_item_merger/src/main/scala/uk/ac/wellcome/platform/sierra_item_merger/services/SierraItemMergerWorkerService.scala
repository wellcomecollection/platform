package uk.ac.wellcome.platform.sierra_item_merger.services

import akka.actor.ActorSystem
import com.google.inject.Inject
import uk.ac.wellcome.messaging.sqs.SQSToDynamoStream
import uk.ac.wellcome.models.transformable.sierra.SierraItemRecord
import uk.ac.wellcome.utils.JsonUtil._

import scala.concurrent.Future

class SierraItemMergerWorkerService @Inject()(
  system: ActorSystem,
  sqsStream: SQSToDynamoStream[SierraItemRecord],
  sierraItemMergerUpdaterService: SierraItemMergerUpdaterService
) {

  sqsStream.foreach(this.getClass.getSimpleName, store)

  private def store(record: SierraItemRecord): Future[Unit] =
    sierraItemMergerUpdaterService.update(record)

  def stop() = system.terminate()
}
