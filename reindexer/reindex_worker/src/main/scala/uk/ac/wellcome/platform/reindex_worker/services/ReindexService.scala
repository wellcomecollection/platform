package uk.ac.wellcome.platform.reindex_worker.services

import com.google.inject.Inject
import com.twitter.inject.Logging
import uk.ac.wellcome.messaging.sns.SNSWriter
import uk.ac.wellcome.platform.reindex_worker.GlobalExecutionContext.context
import uk.ac.wellcome.platform.reindex_worker.models.{ReindexJob, ReindexRequest, ReindexableRecord}
import uk.ac.wellcome.utils.JsonUtil._

import scala.concurrent.Future

class ReindexService @Inject()(readerService: ReindexRecordReaderService,
                               snsWriter: SNSWriter)
    extends Logging {

  def runReindex(reindexJob: ReindexJob): Future[List[Unit]] = {
    val outdatedRecordsFuture = readerService.findRecordsForReindexing(reindexJob)

    // Then we send an SNS notification for all of the records.  Another
    // application will pick these up and do the writes back to DynamoDB.
    outdatedRecordsFuture.flatMap { outdatedRecords: List[ReindexableRecord] =>
      Future.sequence {
        outdatedRecords.map {
          sendIndividualNotification(_, desiredVersion = reindexJob.desiredVersion)
        }
      }
    }
  }

  private def sendIndividualNotification(record: ReindexableRecord,
                                         desiredVersion: Int): Future[Unit] = {
    val request = ReindexRequest(
      id = record.id,
      desiredVersion = desiredVersion
    )

    for {
      _ <- snsWriter.writeMessage(
        message = toJson(request).get,
        subject = this.getClass.getSimpleName
      )
    } yield ()
  }
}
