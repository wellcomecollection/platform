package uk.ac.wellcome.platform.recorder.services

import akka.actor.{ActorSystem, Terminated}
import com.google.inject.Inject
import uk.ac.wellcome.messaging.message.MessageStream
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
  system: ActorSystem) {

  implicit val executionContext: ExecutionContextExecutor = system.dispatcher

  messageStream.foreach(this.getClass.getSimpleName, processMessage)

  private def processMessage(work: TransformedBaseWork): Future[Unit] =
    versionedHybridStore.updateRecord(id(work))((work, EmptyMetadata()))(
      (existingWork, existingMetadata) =>
        if (existingWork.version > work.version) {
          (existingWork, existingMetadata)
        } else {
          (work, EmptyMetadata())
      }
    ).map { _ => () }

  def id(work: TransformedBaseWork) = work.sourceIdentifier.toString

  def stop(): Future[Terminated] = {
    system.terminate()
  }
}
