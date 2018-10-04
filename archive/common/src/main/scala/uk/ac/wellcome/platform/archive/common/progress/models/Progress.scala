package uk.ac.wellcome.platform.archive.common.progress.models

import java.net.URI
import java.time.Instant
import java.util.UUID

import com.gu.scanamo.DynamoFormat
import com.gu.scanamo.error.TypeCoercionError
import io.circe.{Decoder, Encoder, Json}
import uk.ac.wellcome.platform.archive.common.json.URIConverters
import uk.ac.wellcome.platform.archive.common.progress.models.Progress.{
  Completed,
  CompletedCallbackFailed,
  CompletedCallbackSucceeded,
  CompletedNoCallbackProvided,
  Failed,
  None,
  Processing
}

case class Progress(
  id: UUID,
  uploadUri: URI,
  callbackUri: Option[URI],
  result: Progress.Status = Progress.None,
  createdAt: Instant = Instant.now,
  updatedAt: Instant = Instant.now,
  events: Seq[ProgressEvent] = Seq.empty
) {

  def update(progressUpdate: ProgressUpdate) = {
    this.copy(
      result = progressUpdate.status,
      events = progressUpdate.events ++ this.events
    )
  }
}

trait StatusConverters {

  import uk.ac.wellcome.json.JsonUtil.{fromJson, toJson}

  implicit val enc = Encoder.instance[Progress.Status](_ match {
    case None       => Json.fromString("none")
    case Processing => Json.fromString("processing")
    case Completed  => Json.fromString("completed")
    case Failed     => Json.fromString("failed")

    case CompletedNoCallbackProvided =>
      Json.fromString("completed-callback-none")
    case CompletedCallbackSucceeded =>
      Json.fromString("completed-callback-success")
    case CompletedCallbackFailed =>
      Json.fromString("completed-callback-failure")
  })

  implicit val dec = Decoder.instance[Progress.Status](cursor =>
    for {
      status <- cursor.value.as[String]
    } yield {
      status match {
        case "none"       => None
        case "processing" => Processing
        case "completed"  => Completed
        case "failed"     => Failed

        case "completed-callback-none" =>
          CompletedNoCallbackProvided
        case "completed-callback-success" =>
          CompletedCallbackSucceeded
        case "completed-callback-failure" =>
          CompletedCallbackFailed
      }
  })

  implicit val fmtStatus =
    DynamoFormat.xmap[Progress.Status, String](
      fromJson[Progress.Status](_)(dec).toEither.left
        .map(TypeCoercionError)
    )(
      toJson[Progress.Status](_).get
    )
}

object Progress extends URIConverters with StatusConverters {

  sealed trait Status

  case object None extends Status

  case object Processing extends Status

  case object Completed extends Status

  case object Failed extends Status

  case object CompletedNoCallbackProvided extends Status

  case object CompletedCallbackSucceeded extends Status

  case object CompletedCallbackFailed extends Status

  def apply(createRequest: ProgressCreateRequest): Progress = {
    Progress(
      id = generateId,
      uploadUri = createRequest.uploadUri,
      callbackUri = createRequest.callbackUri)
  }

  private def generateId = UUID.randomUUID()
}

case class ProgressEvent(description: String, time: Instant = Instant.now)

case class ProgressUpdate(id: UUID,
                          events: List[ProgressEvent],
                          status: Progress.Status = Progress.None)
    extends StatusConverters

case class FailedProgressUpdate(e: Throwable, update: ProgressUpdate)

case class ProgressCreateRequest(uploadUri: URI, callbackUri: Option[URI])

object ProgressCreateRequest extends URIConverters
