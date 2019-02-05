package uk.ac.wellcome.platform.archive.common.progress.models

import java.util.UUID

import uk.ac.wellcome.platform.archive.common.models.bagit.BagId
import uk.ac.wellcome.platform.archive.common.models.error.ArchiveError

sealed trait ProgressUpdate {
  val id: UUID
  val events: Seq[ProgressEvent]
}
object ProgressUpdate {

  def failed[T](id: UUID, error: ArchiveError[T]) =
    ProgressStatusUpdate(
      id,
      Progress.Failed,
      None,
      List(ProgressEvent(error.toString))
    )

  def event(id: UUID, description: String) =
    ProgressEventUpdate(id, Seq(ProgressEvent(description)))

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
