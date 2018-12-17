package uk.ac.wellcome.platform.archive.common.progress.models

import java.util.UUID

import uk.ac.wellcome.platform.archive.common.models.error.ArchiveError
import uk.ac.wellcome.platform.archive.common.models.{BagId, IngestBagRequest}

sealed trait ProgressUpdate {
  val id: UUID
  val events: Seq[ProgressEvent]
}

case class ProgressEventUpdate(id: UUID, events: Seq[ProgressEvent])
    extends ProgressUpdate

case class ProgressStatusUpdate(id: UUID,
                                status: Progress.Status,
                                affectedBag: Option[BagId],
                                events: Seq[ProgressEvent] = List.empty)
    extends ProgressUpdate

case class ProgressCallbackStatusUpdate(id: UUID,
                                        callbackStatus: Callback.CallbackStatus,
                                        events: Seq[ProgressEvent] = List.empty)
    extends ProgressUpdate

case class FailedProgressUpdate(e: Throwable, update: ProgressUpdate)
