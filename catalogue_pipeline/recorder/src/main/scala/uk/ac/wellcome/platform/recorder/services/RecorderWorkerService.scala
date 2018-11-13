package uk.ac.wellcome.platform.recorder.services

import akka.Done
import com.google.inject.Inject
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

import scala.concurrent.{ExecutionContext, Future}

class RecorderWorkerService @Inject()(
  versionedHybridStore: VersionedHybridStore[TransformedBaseWork,
                                             EmptyMetadata,
                                             ObjectStore[TransformedBaseWork]],
  messageStream: MessageStream[TransformedBaseWork],
  snsWriter: SNSWriter)(implicit executionContext: ExecutionContext) {

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
    work: TransformedBaseWork): Future[VHSIndexEntry[EmptyMetadata]] = {
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
}
