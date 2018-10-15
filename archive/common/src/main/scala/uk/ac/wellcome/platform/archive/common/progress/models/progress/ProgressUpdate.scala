package uk.ac.wellcome.platform.archive.common.progress.models.progress

import java.util.UUID

case class ProgressUpdateRequest(id: UUID, progressUpdate: ProgressUpdate)

sealed trait ProgressUpdate {
  val events: List[ProgressEvent]
}

case class ProgressEventUpdate(events: List[ProgressEvent])
    extends ProgressUpdate

case class ProgressStatusUpdate(status: Progress.Status,
                                events: List[ProgressEvent] = List.empty)
    extends ProgressUpdate
    with StatusConverters

case class ProgressResourceUpdate(affectedResources: List[Resource],
                                  events: List[ProgressEvent] = List.empty)
    extends ProgressUpdate

case class ProgressCallbackStatusUpdate(
  callbackStatus: Callback.Status,
  events: List[ProgressEvent] = List.empty)
    extends ProgressUpdate

case class FailedProgressUpdate(e: Throwable, update: ProgressUpdate)

case class ResourceIdentifier(underlying: String) extends AnyVal

case class Resource(id: ResourceIdentifier)
