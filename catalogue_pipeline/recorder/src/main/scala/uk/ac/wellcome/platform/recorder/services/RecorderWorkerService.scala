package uk.ac.wellcome.platform.recorder.services

import akka.actor.{ActorSystem, Terminated}
import com.google.inject.Inject
import uk.ac.wellcome.messaging.message.MessageStream
import uk.ac.wellcome.models.recorder.internal.RecorderWorkEntry
import uk.ac.wellcome.models.work.internal.UnidentifiedWork
import uk.ac.wellcome.storage.s3.S3TypeStore
import uk.ac.wellcome.storage.vhs.VersionedHybridStore

import uk.ac.wellcome.utils.JsonUtil._
import uk.ac.wellcome.storage.dynamo._

import scala.concurrent.Future

class RecorderWorkerService @Inject()(
  versionedHybridStore: VersionedHybridStore[RecorderWorkEntry,
                                             S3TypeStore[RecorderWorkEntry]],
  messageStream: MessageStream[UnidentifiedWork],
  system: ActorSystem) {

  messageStream.foreach(this.getClass.getSimpleName, processMessage)

  def processMessage(work: UnidentifiedWork): Future[Unit] = {
    val newRecorderEntry = RecorderWorkEntry(work)

    versionedHybridStore.updateRecord(newRecorderEntry.id)(newRecorderEntry)(
      existingEntry =>
        if (existingEntry.work.version > newRecorderEntry.work.version) {
          existingEntry
        } else { newRecorderEntry }
    )()
  }

  def stop(): Future[Terminated] = {
    system.terminate()
  }
}
