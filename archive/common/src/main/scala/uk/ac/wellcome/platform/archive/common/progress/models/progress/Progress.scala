package uk.ac.wellcome.platform.archive.common.progress.models.progress

import java.net.URI
import java.time.Instant
import java.util.UUID

import com.gu.scanamo.DynamoFormat
import com.gu.scanamo.error.TypeCoercionError
import io.circe.{Decoder, Encoder, Json}
import uk.ac.wellcome.platform.archive.common.json.URIConverters

case class Progress(
  id: UUID,
  uploadUri: URI,
  callback: Option[Callback],
  status: Progress.Status,
  createdDate: Instant = Instant.now,
  lastModifiedDate: Instant = Instant.now,
  events: Seq[ProgressEvent] = Seq.empty
) {

  def update(progressUpdate: ProgressUpdate) = {
    this.copy(
      events = progressUpdate.events ++ this.events
    )
  }
}

case object Progress extends URIConverters with StatusConverters {

  sealed trait Status

  case object Initialised extends Status {
    override def toString: String = "initialised"
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

  def apply(createRequest: ProgressCreateRequest): Progress = {
    Progress(
      id = generateId,
      uploadUri = createRequest.uploadUri,
      callback = createRequest.callbackUri.map(Callback(_)),
      status = Progress.Initialised)
  }

  private def generateId: UUID = UUID.randomUUID()
}

trait StatusConverters {
  import uk.ac.wellcome.json.JsonUtil.{fromJson, toJson}
  import Progress._

  implicit val enc: Encoder[Status] = Encoder.instance[Progress.Status] {
    status: Progress.Status =>
      Json.fromString(status.toString)
  }

  implicit val dec: Decoder[Status] =
    Decoder.instance[Progress.Status](cursor =>
      for {
        status <- cursor.value.as[String]
      } yield {
        status match {
          case "processing" => Processing
          case "completed"  => Completed
          case "failed"     => Failed
        }
    })

  implicit val fmtStatus: AnyRef with DynamoFormat[Status] =
    DynamoFormat.xmap[Progress.Status, String](
      fromJson[Progress.Status](_)(dec).toEither.left
        .map(TypeCoercionError)
    )(
      toJson[Progress.Status](_).get
    )
}
