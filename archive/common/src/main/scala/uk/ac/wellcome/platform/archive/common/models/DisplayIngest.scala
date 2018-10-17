package uk.ac.wellcome.platform.archive.common.models

import io.circe.generic.extras.JsonKey
import uk.ac.wellcome.platform.archive.common.progress.models.progress._

case class DisplayIngest(id: String,
                         uploadUrl: String,
                         callback: Option[DisplayCallback],
                         ingestType: DisplayIngestType,
                         space: DisplayStorageSpace,
                         status: DisplayIngestStatus,
                         resources: Seq[DisplayIngestResource],
                         events: Seq[DisplayProgressEvent] = Seq.empty,
                         createdDate: String,
                         lastModifiedDate: String,
                         @JsonKey("type")
                         ontologyType: String = "Ingest")

case class DisplayCallback(callbackUri: String,
                           callbackStatus: String,
                           @JsonKey("type")
                           ontologyType: String = "Callback")

case class DisplayIngestType(id: String = "create",
                             @JsonKey("type")
                             ontologyType: String = "IngestType")

case class DisplayIngestResource(id: String,
                                 @JsonKey("type")
                                 ontologyType: String = "IngestResource")

case class DisplayStorageSpace(id: String,
                               @JsonKey("type")
                               ontologyType: String = "Space")


case class DisplayIngestStatus(id: String,
                               @JsonKey("type")
                               ontologyType: String = "IngestStatus")

case class DisplayProgressEvent(description: String,
                                createdDate: String,
                                @JsonKey("type")
                                ontologyType: String = "ProgressEvent")

case object DisplayIngest {
  def apply(progress: Progress): DisplayIngest = {
    DisplayIngest(
      id = progress.id.toString,
      uploadUrl = progress.uploadUri.toString,
      callback = progress.callback.map(DisplayCallback(_)),
      space = DisplayStorageSpace(progress.space.toString),
      ingestType = DisplayIngestType(),
      resources = progress.resources.map(DisplayIngestResource(_)),
      status = DisplayIngestStatus(progress.status),
      events = progress.events.map(DisplayProgressEvent(_)),
      createdDate = progress.createdDate.toString,
      lastModifiedDate = progress.lastModifiedDate.toString
    )
  }
}

case object DisplayProgressEvent {
  def apply(progressEvent: ProgressEvent): DisplayProgressEvent = {
    DisplayProgressEvent(
      progressEvent.description,
      progressEvent.createdDate.toString)
  }
}

case object DisplayIngestStatus {
  def apply(progressStatus: Progress.Status): DisplayIngestStatus = {
    DisplayIngestStatus(progressStatus.toString)
  }
}

case object DisplayIngestResource {
  def apply(resource: Resource): DisplayIngestResource = {
    DisplayIngestResource(resource.id.underlying)
  }
}

case object DisplayCallback {
  def apply(callback: Callback): DisplayCallback = {
    DisplayCallback(
      callback.uri.toString,
      callback.status.toString
    )
  }
}
