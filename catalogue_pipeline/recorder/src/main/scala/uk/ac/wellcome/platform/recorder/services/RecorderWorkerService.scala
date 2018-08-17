package uk.ac.wellcome.platform.recorder.services

import akka.actor.{ActorSystem, Terminated}
import com.google.inject.Inject
import uk.ac.wellcome.messaging.message.{MessagePointer, MessageStream}
import uk.ac.wellcome.models.work.internal.TransformedBaseWork
import uk.ac.wellcome.storage.{ObjectLocation, ObjectStore}
import uk.ac.wellcome.storage.dynamo._
import uk.ac.wellcome.storage.vhs.{EmptyMetadata, HybridRecord, VHSConfig, VersionedHybridStore}

import scala.concurrent.{ExecutionContextExecutor, Future}

class RecorderWorkerService @Inject()(
  versionedHybridStore: VersionedHybridStore[TransformedBaseWork,
                                             EmptyMetadata,
                                             ObjectStore[TransformedBaseWork]],
  messageStream: MessageStream[TransformedBaseWork],
  vHSConfig: VHSConfig,
  system: ActorSystem) {

  implicit val executionContext: ExecutionContextExecutor = system.dispatcher

  messageStream.foreach(this.getClass.getSimpleName, processMessage)

  private def processMessage(work: TransformedBaseWork): Future[Unit] =
    for {
     _ <- storeInVhs(work)
      maybeHybridRecord <- versionedHybridStore.versionedDao.getRecord[HybridRecord](work.sourceIdentifier.toString)
    } yield (messagePointer(maybeHybridRecord))

  private def messagePointer(maybeHybridRecord: Option[HybridRecord]) = {
    val hybridRecord = maybeHybridRecord.getOrElse(throw new RuntimeException("Boom"))
    MessagePointer(ObjectLocation(namespace = vHSConfig.s3Config.bucketName, key = hybridRecord.s3key))
  }

  private def storeInVhs(work: TransformedBaseWork) = {
    versionedHybridStore.updateRecord(work.sourceIdentifier.toString)((work, EmptyMetadata()))(
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
