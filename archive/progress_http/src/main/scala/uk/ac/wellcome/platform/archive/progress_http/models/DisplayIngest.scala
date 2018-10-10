package uk.ac.wellcome.platform.archive.progress_http.models

import io.circe.generic.extras.JsonKey
import uk.ac.wellcome.platform.archive.common.progress.models.{
  Progress,
  ProgressEvent
}
import uk.ac.wellcome.platform.archive.common.progress.models.Progress._

case class DisplayIngest(id: String,
                         uploadUrl: String,
                         callbackUrl: Option[String],
                         ingestType: DisplayIngestType,
                         status: DisplayIngestStatus,
                         events: Seq[DisplayProgressEvent] = Seq.empty,
                         createdDate: String,
                         lastModifiedDate: String,
                         @JsonKey("type") ontologyType: String = "Ingest")

case class DisplayIngestType(id: String = "create",
                             @JsonKey("type") ontologyType: String =
                               "IngestType")

case class DisplayIngestStatus(id: String,
                               @JsonKey("type") ontologyType: String =
                                 "IngestStatus")

case class DisplayProgressEvent(description: String,
                                time: String,
                                @JsonKey("type") ontologyType: String =
                                  "ProgressEvent")

case object DisplayIngest {
  def apply(progress: Progress): DisplayIngest = {
    DisplayIngest(
      id = progress.id.toString,
      uploadUrl = progress.uploadUri.toString,
      callbackUrl = progress.callbackUri.map(_.toString),
      ingestType = DisplayIngestType(),
      status = DisplayIngestStatus(progress.status),
      events = progress.events.map(DisplayProgressEvent(_)),
      createdDate = progress.createdDate.toString,
      lastModifiedDate = progress.lastModifiedDate.toString
    )
  }
}

case object DisplayProgressEvent {
  def apply(progressEvent: ProgressEvent): DisplayProgressEvent = {
    DisplayProgressEvent(progressEvent.description, progressEvent.time.toString)
  }
}

case object DisplayIngestStatus {
  def apply(progressStatus: Status): DisplayIngestStatus = {
    DisplayIngestStatus(progressStatus.toString)
  }
}
