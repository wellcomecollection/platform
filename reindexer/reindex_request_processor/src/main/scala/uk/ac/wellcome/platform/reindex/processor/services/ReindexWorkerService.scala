package uk.ac.wellcome.platform.reindex.processor.services

import akka.actor.{ActorSystem, Terminated}
import com.google.inject.Inject
import grizzled.slf4j.Logging
import uk.ac.wellcome.messaging.sns.NotificationMessage
import uk.ac.wellcome.messaging.sqs.SQSStream
import uk.ac.wellcome.models.reindexer.{ReindexRequest, ReindexableRecord}
import uk.ac.wellcome.storage.dynamo._
import uk.ac.wellcome.utils.JsonUtil._

import scala.concurrent.{ExecutionContextExecutor, Future}

class ReindexWorkerService @Inject()(versionedDao: VersionedDao,
                                     sqsStream: SQSStream[NotificationMessage],
                                     system: ActorSystem)
    extends Logging {

  implicit val executionContext: ExecutionContextExecutor = system.dispatcher

  sqsStream.foreach(this.getClass.getSimpleName, processMessage)

  private def processMessage(message: NotificationMessage): Future[Unit] =
    for {
      reindexRequest <- Future.fromTry(fromJson[ReindexRequest](message.Message))
      maybeReindexableRecord <- versionedDao.getRecord[ReindexableRecord](reindexRequest.id)
      _ <- maybeReindexableRecord match {
        case Some(existingRecord) =>
          if (reindexRequest.desiredVersion > existingRecord.reindexVersion) {
            updateRecord(reindexRequest, existingRecord)
          } else {
            Future.successful(())
          }
        case None =>
          throw new RuntimeException(s"VersionedDao has no record for $reindexRequest")
        }
    } yield ()

  private def updateRecord(reindexRequest: ReindexRequest, existingRecord: ReindexableRecord) = {
    val mergedRecord = existingRecord.copy(
      reindexVersion = reindexRequest.desiredVersion)
    versionedDao.updateRecord(mergedRecord)
  }

  def stop(): Future[Terminated] = system.terminate()
}
