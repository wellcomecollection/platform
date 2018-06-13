package uk.ac.wellcome.platform.recorder.services

import akka.actor.{ActorSystem, Terminated}
import com.google.inject.Inject
import uk.ac.wellcome.messaging.message.MessageStream
import uk.ac.wellcome.models.recorder.internal.RecorderWorkEntry
import uk.ac.wellcome.models.work.internal.UnidentifiedWork
import uk.ac.wellcome.storage.ObjectStore
import uk.ac.wellcome.storage.dynamo._
import uk.ac.wellcome.storage.vhs.{EmptyMetadata, VersionedHybridStore}

import scala.concurrent.{ExecutionContextExecutor, Future}

class RecorderWorkerService @Inject()(
  versionedHybridStore: VersionedHybridStore[RecorderWorkEntry,
                                             EmptyMetadata,
                                             ObjectStore[RecorderWorkEntry]],
  messageStream: MessageStream[UnidentifiedWork],
  system: ActorSystem) {

  implicit val executionContext: ExecutionContextExecutor = system.dispatcher

  messageStream.foreach(this.getClass.getSimpleName, processMessage)

  def processMessage(work: UnidentifiedWork): Future[Unit] = {
    val newRecorderEntry = RecorderWorkEntry(work)

    versionedHybridStore.updateRecord(newRecorderEntry.id)(
      (newRecorderEntry, EmptyMetadata()))(
      (existingEntry, existingMetadata) =>
        if (existingEntry.work.version > newRecorderEntry.work.version) {
          (existingEntry, existingMetadata)
        } else {
          (newRecorderEntry, EmptyMetadata())
      }
    )
  }

  def stop(): Future[Terminated] = {
    system.terminate()
  }
}
