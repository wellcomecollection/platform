package uk.ac.wellcome.platform.archive.common.progress.models

import java.time.Instant
import java.util.UUID

import io.circe.{Decoder, Encoder, Json}


case class Progress(
  id: String,
  uploadUrl: String,
  callbackUrl: Option[String],
  result: Progress.Status = Progress.None,
  createdAt: Instant = Instant.now,
  updatedAt: Instant = Instant.now,
  events: Seq[ProgressEvent] = Seq.empty
) {
  def update(progressUpdate: ProgressUpdate) = {
    this.copy(
      result = progressUpdate.status,
      events = progressUpdate.event +: this.events
    )
  }
}

object Progress {
  implicit val statusEncoder = Encoder.instance[Progress.Status](
    _ match {
        case None => Json.fromString("none")
        case Processing => Json.fromString("processing")
        case Completed => Json.fromString("completed")
        case Failed => Json.fromString("failed")
      })

  implicit val licenseDecoder = Decoder.instance[Progress.Status](cursor =>
    for {
      status <- cursor.downField("result").as[String]
    } yield {
      status match {
        case "none" => None
        case "processing" => Processing
        case "completed" => Completed
        case "failed" => Failed
      }
    })

  sealed trait Status
  case object None extends Status
  case object Processing extends Status
  case object Completed extends Status
  case object Failed extends Status

  def apply(createRequest: ProgressCreateRequest): Progress = {
    Progress(
      id = generateId,
      uploadUrl = createRequest.uploadUrl,
      callbackUrl = createRequest.callbackUrl
    )
  }

  private def generateId = UUID.randomUUID().toString
}

case class ProgressEvent(description: String, time: Instant = Instant.now)

case class ProgressUpdate(id: String,
                          event: ProgressEvent,
                          status: Progress.Status = Progress.None)
case class FailedProgressUpdate(e: Throwable, update: ProgressUpdate)

case class ProgressCreateRequest(uploadUrl: String, callbackUrl: Option[String])
