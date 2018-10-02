package uk.ac.wellcome.platform.archive.common.progress.models

import java.time.Instant

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
  sealed trait Status
  case object None extends Status
  case object Processing extends Status
  case object Completed extends Status
  case object Failed extends Status
  case object CompletedNoCallbackProvided extends Status
  case object CompletedCallbackSucceeded extends Status
  case object CompletedCallbackFailed extends Status
}

case class ProgressEvent(description: String, time: Instant = Instant.now)

case class ProgressUpdate(id: String,
                          event: ProgressEvent,
                          status: Progress.Status = Progress.None)
case class FailedProgressUpdate(e: Throwable, update: ProgressUpdate)
