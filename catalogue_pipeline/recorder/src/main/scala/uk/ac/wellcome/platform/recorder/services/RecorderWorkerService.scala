package uk.ac.wellcome.platform.recorder.services

import akka.actor.{ActorSystem, Terminated}
import com.google.inject.Inject
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.messaging.message.MessageStream
import uk.ac.wellcome.messaging.sns.SNSWriter
import uk.ac.wellcome.models.work.internal.TransformedBaseWork
import uk.ac.wellcome.storage.ObjectStore
import uk.ac.wellcome.storage.dynamo._
import uk.ac.wellcome.storage.vhs.{EmptyMetadata, VersionedHybridStore}

import scala.concurrent.{ExecutionContextExecutor, Future}

class RecorderWorkerService @Inject()(
  versionedHybridStore: VersionedHybridStore[TransformedBaseWork,
                                             EmptyMetadata,
                                             ObjectStore[TransformedBaseWork]],
  messageStream: MessageStream[TransformedBaseWork],
  snsWriter: SNSWriter,
  system: ActorSystem) {

  implicit val executionContext: ExecutionContextExecutor = system.dispatcher

  messageStream.foreach(this.getClass.getSimpleName, processMessage)

  private def processMessage(work: TransformedBaseWork): Future[Unit] =
    for {
      (hybridRecord, _) <- storeInVhs(work)
      _ <- snsWriter.writeMessage(
        message = hybridRecord.location,
        subject = s"Sent from ${this.getClass.getSimpleName}")
    } yield ()

  private def storeInVhs(work: TransformedBaseWork) = {
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

  def stop(): Future[Terminated] = {
    system.terminate()
  }
}
