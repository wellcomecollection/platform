package uk.ac.wellcome.platform.storage.ingests.api

import uk.ac.wellcome.messaging.sns.SNSWriter
import uk.ac.wellcome.platform.archive.common.models.{
  IngestBagRequest,
  StorageSpace
}
import uk.ac.wellcome.platform.archive.common.progress.models.Progress
import uk.ac.wellcome.platform.archive.common.progress.monitor.ProgressTracker
import uk.ac.wellcome.json.JsonUtil._

import scala.concurrent.{ExecutionContext, Future}

class ProgressStarter(progressTracker: ProgressTracker, snsWriter: SNSWriter)(
  implicit ec: ExecutionContext) {
  def initialise(progress: Progress): Future[Progress] =
    for {
      progress <- progressTracker.initialise(progress)
      _ <- snsWriter.writeMessage(
        toIngestRequest(progress),
        "progress-http-request-created")
    } yield progress

  private def toIngestRequest(progress: Progress): IngestBagRequest =
    IngestBagRequest(
      id = progress.id,
      zippedBagLocation = progress.sourceLocation.location,
      archiveCompleteCallbackUrl = progress.callback.map { _.uri },
      storageSpace = StorageSpace(progress.space.underlying)
    )
}
