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
  status: Progress.Status = Progress.Initialised,
  resources: Seq[Resource] = Seq.empty,
  createdDate: Instant = Instant.now,
  lastModifiedDate: Instant = Instant.now,
  events: Seq[ProgressEvent] = Seq.empty
) {

  def update(update: ProgressUpdate): Progress = {
    val mergedEvents = update.events ++ this.events
    update match {
      case _: ProgressEventUpdate => this.copy(
          events = mergedEvents)
      case statusUpdate: ProgressStatusUpdate => this.copy(
          status = statusUpdate.status,
          events = mergedEvents)
      case resourceUpdate: ProgressResourceUpdate => this.copy(
          resources = this.resources ++ resourceUpdate.affectedResources,
          events = mergedEvents
        )
      case callbackStatusUpdate: ProgressCallbackStatusUpdate => this.copy(
          callback = this.callback.map(
            _.copy(callbackStatus = callbackStatusUpdate.callbackStatus)),
          events = mergedEvents
        )
    }
  }
}

case object Progress extends URIConverters with StatusConverters {
  sealed trait Status

  private val initialisedString = "initialised"
  private val processingString = "processing"
  private val completedString = "completed"
  private val failedString = "failed"

  case object Initialised extends Status {
    override def toString: String = initialisedString
  }

  case object Processing extends Status {
    override def toString: String = processingString
  }

  case object Completed extends Status {
    override def toString: String = completedString
  }

  case object Failed extends Status {
    override def toString: String = failedString
  }

  def apply(createRequest: ProgressCreateRequest): Progress = {
    Progress(
      id = generateId,
      uploadUri = createRequest.uploadUri,
      callback = createRequest.callbackUri.map(Callback(_)),
      status = Progress.Initialised)
  }

  def parseStatus(status: String) : Status = {
    status match {
      case `initialisedString` => Initialised
      case `processingString` => Processing
      case `completedString` => Completed
      case `failedString` => Failed
    }
  }

  private def generateId: UUID = UUID.randomUUID()
}

trait StatusConverters {
  import uk.ac.wellcome.json.JsonUtil.{fromJson, toJson}
  import Progress._

  implicit val encoder: Encoder[Status] = Encoder.instance[Progress.Status] {
    status: Progress.Status =>
      Json.fromString(status.toString)
  }

  implicit val decoder: Decoder[Status] =
    Decoder.instance[Progress.Status](cursor =>
      for {
        status <- cursor.value.as[String]
      } yield {
        parseStatus(status)
    })

  implicit val fmtStatus: AnyRef with DynamoFormat[Status] =
    DynamoFormat.xmap[Progress.Status, String](
      fromJson[Progress.Status](_)(decoder).toEither.left
        .map(TypeCoercionError)
    )(
      toJson[Progress.Status](_).get
    )
}
