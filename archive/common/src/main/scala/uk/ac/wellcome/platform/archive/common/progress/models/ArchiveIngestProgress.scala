package uk.ac.wellcome.platform.archive.common.progress.models

import java.time.Instant

case class ArchiveIngestProgress (
  id: String,
  uploadUrl: String,
  callbackUrl: String,
  result: ArchiveIngestProgress.ArchiveResultStatus = ArchiveIngestProgress.None,
  createdAt: Instant = Instant.now,
  updatedAt: Instant = Instant.now,
  events: Seq[ProgressEvent] = Seq.empty
)

object ArchiveIngestProgress {
  sealed trait ArchiveResultStatus
  case object None extends ArchiveResultStatus
  case object Completed extends ArchiveResultStatus
  case object Failed extends ArchiveResultStatus
}

case class ProgressEvent(description: String, time: Instant)