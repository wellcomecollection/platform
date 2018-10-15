package uk.ac.wellcome.platform.archive.common.progress.models.progress

import java.util.UUID

case class ResourceIdentifier(underlying: String) extends AnyVal

case class Resource(id: ResourceIdentifier)

sealed trait ProgressUpdate {
  val id: UUID
  val events: List[ProgressEvent]
}

case class ProgressEventUpdate(id: UUID,
                               events: List[ProgressEvent])
  extends ProgressUpdate

case class ProgressStatusUpdate(id: UUID,
                                status: Progress.Status,
                                events: List[ProgressEvent] = List.empty)
  extends ProgressUpdate with StatusConverters

case class ProgressResourceUpdate(id: UUID,
                                  affectedResources: List[Resource],
                                  events: List[ProgressEvent] = List.empty)
  extends ProgressUpdate

case class ProgressCallbackStatusUpdate(id: UUID,
                                        callbackStatus: Callback.Status,
                                        events: List[ProgressEvent] = List.empty)
  extends ProgressUpdate

case class FailedProgressUpdate(e: Throwable, update: ProgressUpdate)
