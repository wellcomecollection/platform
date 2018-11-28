package uk.ac.wellcome.platform.archive.display

import java.net.{URI, URL}
import java.util.UUID

import io.circe.generic.extras.JsonKey
import uk.ac.wellcome.platform.archive.common.models.{BagId, Namespace}
import uk.ac.wellcome.platform.archive.common.progress.models._

sealed trait DisplayIngest

case class RequestDisplayIngest(sourceLocation: DisplayLocation,
                                callback: Option[DisplayCallback],
                                ingestType: DisplayIngestType,
                                space: DisplaySpace,
                                @JsonKey("type")
                                ontologyType: String = "Ingest")
    extends DisplayIngest {
  def toProgress: Progress = {
    Progress(
      id = UUID.randomUUID,
      sourceLocation = sourceLocation.toStorageLocation,
      callback = Callback(
        callback.map(displayCallback => URI.create(displayCallback.url))),
      space = Namespace(space.id),
      status = Progress.Accepted
    )
  }
}

case class ResponseDisplayIngest(@JsonKey("@context")
                                 context: String,
                                 id: UUID,
                                 sourceLocation: DisplayLocation,
                                 callback: Option[DisplayCallback],
                                 ingestType: DisplayIngestType,
                                 space: DisplaySpace,
                                 status: DisplayStatus,
                                 bag: Option[IngestDisplayBag] = None,
                                 events: Seq[DisplayProgressEvent] = Seq.empty,
                                 createdDate: String,
                                 lastModifiedDate: String,
                                 @JsonKey("type")
                                 ontologyType: String = "Ingest")
    extends DisplayIngest

case class IngestDisplayBag(id: String,
                            @JsonKey("type")
                            ontologyType: String = "Bag")

case class DisplayCallback(url: String,
                           status: Option[DisplayStatus],
                           @JsonKey("type")
                           ontologyType: String = "Callback")

case class DisplayIngestType(id: String = "create",
                             @JsonKey("type")
                             ontologyType: String = "IngestType")

case class DisplayProgressEvent(description: String,
                                createdDate: String,
                                @JsonKey("type")
                                ontologyType: String = "ProgressEvent")

case object ResponseDisplayIngest {
  def apply(progress: Progress, contextUrl: URL): ResponseDisplayIngest =
    ResponseDisplayIngest(
      context = contextUrl.toString,
      id = progress.id,
      sourceLocation = DisplayLocation(progress.sourceLocation),
      callback = progress.callback.map(DisplayCallback(_)),
      space = DisplaySpace(progress.space),
      ingestType = DisplayIngestType(),
      bag = progress.bag.map(IngestDisplayBag(_)),
      status = DisplayStatus(progress.status),
      events = progress.events.map(DisplayProgressEvent(_)),
      createdDate = progress.createdDate.toString,
      lastModifiedDate = progress.lastModifiedDate.toString
    )
}

case object DisplayProgressEvent {
  def apply(progressEvent: ProgressEvent): DisplayProgressEvent =
    DisplayProgressEvent(
      progressEvent.description,
      progressEvent.createdDate.toString)
}

case object DisplayCallback {
  def apply(callback: Callback): DisplayCallback = DisplayCallback(
    callback.uri.toString,
    Some(DisplayStatus(callback.status))
  )
}

object IngestDisplayBag {
  def apply(bagId: BagId): IngestDisplayBag = IngestDisplayBag(bagId.toString)
}
