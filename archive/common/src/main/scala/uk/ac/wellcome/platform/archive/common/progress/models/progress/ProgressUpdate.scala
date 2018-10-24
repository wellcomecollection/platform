package uk.ac.wellcome.platform.archive.common.progress.models.progress

import java.util.UUID

sealed trait ProgressUpdate {
  val id: UUID
  val events: Seq[ProgressEvent]
}

case class ProgressEventUpdate(id: UUID, events: Seq[ProgressEvent])
    extends ProgressUpdate

case class ProgressStatusUpdate(id: UUID,
                                status: Progress.Status,
                                affectedResources: Seq[Resource],
                                events: Seq[ProgressEvent] = List.empty)
    extends ProgressUpdate

case class ProgressCallbackStatusUpdate(id: UUID,
                                        callbackStatus: Callback.CallbackStatus,
                                        events: Seq[ProgressEvent] = List.empty)
    extends ProgressUpdate

case class FailedProgressUpdate(e: Throwable, update: ProgressUpdate)

case class Resource(id: ResourceIdentifier)

case class ResourceIdentifier(underlying: String) extends AnyVal {
  override def toString: String = underlying
}
