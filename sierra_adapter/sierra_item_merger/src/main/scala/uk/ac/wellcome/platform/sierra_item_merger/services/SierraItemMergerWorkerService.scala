package uk.ac.wellcome.platform.sierra_item_merger.services

import akka.actor.ActorSystem
import com.google.inject.Inject
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.messaging.message.{MessagePointer, MessageStream}
import uk.ac.wellcome.messaging.sns.SNSWriter
import uk.ac.wellcome.models.transformable.sierra.SierraItemRecord
import uk.ac.wellcome.storage.vhs.HybridRecord

import scala.concurrent.{ExecutionContext, Future}

class SierraItemMergerWorkerService @Inject()(
  system: ActorSystem,
  messageStream: MessageStream[SierraItemRecord],
  sierraItemMergerUpdaterService: SierraItemMergerUpdaterService,
  snsWriter: SNSWriter
)(implicit ec: ExecutionContext) {

  messageStream.foreach(this.getClass.getSimpleName, process)

  private def process(itemRecord: SierraItemRecord): Future[Unit] =
    for {
      hybridRecords: Seq[HybridRecord] <- sierraItemMergerUpdaterService.update(
        itemRecord)
      _ <- Future.sequence(
        hybridRecords.map { record =>
          snsWriter.writeMessage(
            message = MessagePointer(record.location),
            subject = s"Sent from ${this.getClass.getSimpleName}"
          )
        }
      )
    } yield ()

  def stop() = system.terminate()
}
