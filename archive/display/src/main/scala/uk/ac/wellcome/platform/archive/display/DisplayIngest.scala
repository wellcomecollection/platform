package uk.ac.wellcome.platform.archive.display

import java.net.{URI, URL}
import java.util.UUID

import io.circe.generic.extras.JsonKey
import io.swagger.annotations.{ApiModel, ApiModelProperty}
import uk.ac.wellcome.platform.archive.common.models.BagId
import uk.ac.wellcome.platform.archive.common.progress.models._

import scala.annotation.meta.field

sealed trait DisplayIngest

@ApiModel(value = "RequestIngest")
case class RequestDisplayIngest(sourceLocation: DisplayLocation,
                                callback: Option[DisplayCallback],
                                ingestType: DisplayIngestType,
                                space: DisplayStorageSpace,
                                @JsonKey("type")
                                @(ApiModelProperty @field)(name = "type", allowableValues = "Ingest")
                                ontologyType: String = "Ingest")
    extends DisplayIngest {
  def toProgress: Progress = {
    Progress(
      id = UUID.randomUUID,
      sourceLocation = sourceLocation.toStorageLocation,
      callback = Callback(
        callback.map(displayCallback => URI.create(displayCallback.url))),
      space = Namespace(space.id),
      status = Progress.Initialised
    )
  }
}

@ApiModel(value = "ResponseIngest")
case class ResponseDisplayIngest(@JsonKey("@context")
                                 @(ApiModelProperty @field)(name = "@context", required = true)
                                 context: String,
                                 id: UUID,
                                 sourceLocation: DisplayLocation,
                                 callback: Option[DisplayCallback],
                                 ingestType: DisplayIngestType,
                                 space: DisplayStorageSpace,
                                 status: DisplayStatus,
                                 bag: Option[IngestDisplayBag] = None,
                                 events: Seq[DisplayProgressEvent] = Seq.empty,
                                 createdDate: String,
                                 lastModifiedDate: String,
                                 @JsonKey("type")
                                 @(ApiModelProperty @field)(name = "type", allowableValues = "Ingest")
                                 ontologyType: String = "Ingest")
    extends DisplayIngest

@ApiModel(value = "Bag")
case class IngestDisplayBag(id: String,
                            @JsonKey("type")
                            @(ApiModelProperty @field)(name = "type", allowableValues = "Bag")
                            ontologyType: String = "Bag")

@ApiModel(value = "Callback")
case class DisplayCallback(url: String,
                           status: Option[DisplayStatus],
                           @JsonKey("type")
                           @(ApiModelProperty @field)(name="type", allowableValues = "Callback")
                           ontologyType: String = "Callback")

@ApiModel(value = "IngestType")
case class DisplayIngestType(id: String = "create",
                             @JsonKey("type")
                             @(ApiModelProperty @field)(name="type", allowableValues = "IngestType")
                             ontologyType: String = "IngestType")

@ApiModel(value = "Space")
case class DisplayStorageSpace(id: String,
                               @JsonKey("type")
                               @(ApiModelProperty @field)(name="type", allowableValues = "Space")
                               ontologyType: String = "Space")
@ApiModel(value = "Status")
case class DisplayStatus(id: String,
                         @JsonKey("type")
                         @(ApiModelProperty @field)(name="type", allowableValues = "Status")
                         ontologyType: String = "Status")

@ApiModel(value = "ProgressEvent")
case class DisplayProgressEvent(description: String,
                                createdDate: String,
                                @JsonKey("type")
                                @(ApiModelProperty @field)(name="type", allowableValues = "Status")
                                ontologyType: String = "ProgressEvent")

case object ResponseDisplayIngest {
  def apply(progress: Progress, contextUrl: URL): ResponseDisplayIngest =
    ResponseDisplayIngest(
      context = contextUrl.toString,
      id = progress.id,
      sourceLocation = DisplayLocation(progress.sourceLocation),
      callback = progress.callback.map(DisplayCallback(_)),
      space = DisplayStorageSpace(progress.space.toString),
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

case object DisplayStatus {
  def apply(progressStatus: Progress.Status): DisplayStatus =
    DisplayStatus(progressStatus.toString)

  def apply(callbackStatus: Callback.CallbackStatus): DisplayStatus =
    DisplayStatus(callbackStatus.toString)
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
