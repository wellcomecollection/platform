package uk.ac.wellcome.platform.archive.common.progress.models

import java.util.UUID

import uk.ac.wellcome.platform.archive.common.models.BagId
import uk.ac.wellcome.platform.archive.common.models.error.ArchiveError

sealed trait ProgressUpdate {
  val id: UUID
  val events: Seq[ProgressEvent]
}
object ProgressUpdate {
  def failed[T](id: UUID, error: ArchiveError[T]) =
    ProgressStatusUpdate(
      id = id,
      status = Progress.Failed,
      events = List(ProgressEvent(error.toString))
    )

  def event(id: UUID, description: String) =
    ProgressEventUpdate(
      id = id,
      events = Seq(ProgressEvent(description))
    )
}

case class ProgressEventUpdate(id: UUID, events: Seq[ProgressEvent])
    extends ProgressUpdate

case class ProgressStatusUpdate(id: UUID,
                                status: Progress.Status,
                                affectedBag: Option[BagId] = None,
                                events: Seq[ProgressEvent] = List.empty)
    extends ProgressUpdate

case class ProgressCallbackStatusUpdate(id: UUID,
                                        callbackStatus: Callback.CallbackStatus,
                                        events: Seq[ProgressEvent] = List.empty)
    extends ProgressUpdate

case class FailedProgressUpdate(e: Throwable, update: ProgressUpdate)
