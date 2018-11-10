package uk.ac.wellcome.platform.sierra_item_merger.services

import akka.actor.ActorSystem
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.messaging.sns.{NotificationMessage, SNSWriter}
import uk.ac.wellcome.messaging.sqs.SQSStream
import uk.ac.wellcome.models.transformable.sierra.SierraItemRecord
import uk.ac.wellcome.storage.ObjectStore
import uk.ac.wellcome.storage.vhs.{EmptyMetadata, HybridRecord, VHSIndexEntry}

import scala.concurrent.{ExecutionContext, Future}

class SierraItemMergerWorkerService(
  system: ActorSystem,
  sqsStream: SQSStream[NotificationMessage],
  sierraItemMergerUpdaterService: SierraItemMergerUpdaterService,
  objectStore: ObjectStore[SierraItemRecord],
  snsWriter: SNSWriter
)(implicit ec: ExecutionContext) {

  sqsStream.foreach(this.getClass.getSimpleName, process)

  private def process(message: NotificationMessage): Future[Unit] =
    for {
      hybridRecord <- Future.fromTry(fromJson[HybridRecord](message.body))
      itemRecord <- objectStore.get(hybridRecord.location)
      vhsIndexEntries: Seq[VHSIndexEntry[EmptyMetadata]] <- sierraItemMergerUpdaterService
        .update(itemRecord)
      hybridRecords: Seq[HybridRecord] = vhsIndexEntries.map { _.hybridRecord }
      _ <- Future.sequence(
        hybridRecords.map { hybridRecord =>
          snsWriter.writeMessage(
            message = hybridRecord,
            subject = s"Sent from ${this.getClass.getSimpleName}"
          )
        }
      )
    } yield ()

  def stop() = system.terminate()
}
