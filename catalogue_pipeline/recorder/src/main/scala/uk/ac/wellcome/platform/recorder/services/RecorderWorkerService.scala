package uk.ac.wellcome.platform.recorder.services

import akka.Done
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.messaging.message.{
  MessageNotification,
  MessageStream,
  RemoteNotification
}
import uk.ac.wellcome.messaging.sns.SNSWriter
import uk.ac.wellcome.models.work.internal.TransformedBaseWork
import uk.ac.wellcome.storage.ObjectStore
import uk.ac.wellcome.storage.dynamo._
import uk.ac.wellcome.storage.vhs.{
  EmptyMetadata,
  VHSIndexEntry,
  VersionedHybridStore
}
import uk.ac.wellcome.typesafe.Runnable

import scala.concurrent.{ExecutionContext, Future}

class RecorderWorkerService(
  versionedHybridStore: VersionedHybridStore[TransformedBaseWork,
                                             EmptyMetadata,
                                             ObjectStore[TransformedBaseWork]],
  messageStream: MessageStream[TransformedBaseWork],
  snsWriter: SNSWriter)(implicit ec: ExecutionContext)
    extends Runnable {

  def run(): Future[Done] =
    messageStream.foreach(this.getClass.getSimpleName, processMessage)

  private def processMessage(work: TransformedBaseWork): Future[Unit] =
    for {
      vhsEntry <- storeInVhs(work)
      _ <- snsWriter.writeMessage[MessageNotification](
        message = RemoteNotification(vhsEntry.hybridRecord.location),
        subject = s"Sent from ${this.getClass.getSimpleName}")
    } yield ()

  private def storeInVhs(
    work: TransformedBaseWork): Future[VHSIndexEntry[EmptyMetadata]] =
    versionedHybridStore.updateRecord(work.sourceIdentifier.toString)(
      (work, EmptyMetadata()))(
      (existingWork, existingMetadata) =>
        if (existingWork.version > work.version) {
          (existingWork, existingMetadata)
        } else {
          (work, EmptyMetadata())
      }
    )
}
