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
  Failed,
  None,
  Processing
}

case class Progress(
  id: UUID,
  uploadUri: URI,
  callbackUri: Option[URI],
  status: Progress.Status = Progress.None,
  createdDate: Instant = Instant.now,
  lastModifiedDate: Instant = Instant.now,
  events: Seq[ProgressEvent] = Seq.empty
) {

  def update(progressUpdate: ProgressUpdate) = {
    this.copy(
      status = progressUpdate.status,
      events = progressUpdate.events ++ this.events
    )
  }
}

trait StatusConverters {

  import uk.ac.wellcome.json.JsonUtil.{fromJson, toJson}

  implicit val enc = Encoder.instance[Progress.Status] {
    status: Progress.Status =>
      Json.fromString(status.toString)
  }

  implicit val dec = Decoder.instance[Progress.Status](cursor =>
    for {
      status <- cursor.value.as[String]
    } yield {
      status match {
        case "none"       => None
        case "processing" => Processing
        case "completed"  => Completed
        case "failed"     => Failed

        case "completed-callback-succeeded" =>
          CompletedCallbackSucceeded
        case "completed-callback-failed" =>
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

  case object None extends Status {
    override def toString: String = "none"
  }

  case object Processing extends Status {
    override def toString: String = "processing"
  }

  case object Completed extends Status {
    override def toString: String = "completed"
  }

  case object Failed extends Status {
    override def toString: String = "failed"
  }

  case object CompletedCallbackSucceeded extends Status {
    override def toString: String = "completed-callback-succeeded"
  }

  case object CompletedCallbackFailed extends Status {
    override def toString: String = "completed-callback-failed"
  }

  def apply(createRequest: ProgressCreateRequest): Progress = {
    Progress(
      id = generateId,
      uploadUri = createRequest.uploadUri,
      callbackUri = createRequest.callbackUri)
  }

  private def generateId: UUID = UUID.randomUUID()
}

case class ProgressEvent(description: String,
                         createdDate: Instant = Instant.now)

case class ProgressUpdate(id: UUID,
                          events: List[ProgressEvent],
                          status: Progress.Status = Progress.None)
    extends StatusConverters

case class FailedProgressUpdate(e: Throwable, update: ProgressUpdate)

case class ProgressCreateRequest(uploadUri: URI, callbackUri: Option[URI])

object ProgressCreateRequest extends URIConverters
