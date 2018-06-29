package uk.ac.wellcome.platform.reindex_worker.services

import com.google.inject.Inject
import com.twitter.inject.Logging
import uk.ac.wellcome.platform.reindex_worker.GlobalExecutionContext.context
import uk.ac.wellcome.platform.reindex_worker.models.ReindexJob

import scala.concurrent.Future

class ReindexService @Inject()(readerService: RecordReader,
                               notificationService: NotificationSender)
    extends Logging {

  def sendReindexRequests(reindexJob: ReindexJob): Future[List[Unit]] = {
    val outdatedRecordsFuture: Future[List[String]] = readerService.findRecordsForReindexing(reindexJob)

    outdatedRecordsFuture.flatMap { outdatedRecordIds: List[String] =>
      notificationService.sendNotifications(outdatedRecordIds, desiredVersion = reindexJob.desiredVersion)
    }
  }
}
