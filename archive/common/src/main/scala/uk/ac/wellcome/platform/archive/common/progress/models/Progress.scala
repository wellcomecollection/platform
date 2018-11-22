package uk.ac.wellcome.platform.archive.common.progress.models

import java.time.Instant
import java.util.UUID

import uk.ac.wellcome.platform.archive.common.json.URIConverters
import uk.ac.wellcome.platform.archive.common.models.BagId

case class Progress(id: UUID,
                    sourceLocation: StorageLocation,
                    space: Namespace,
                    callback: Option[Callback] = None,
                    status: Progress.Status = Progress.Initialised,
                    bag: Option[BagId] = None,
                    createdDate: Instant = Instant.now,
                    lastModifiedDate: Instant = Instant.now,
                    events: Seq[ProgressEvent] = Seq.empty)

case object Progress extends URIConverters {
  sealed trait Status

  private val initialisedString = "initialised"
  private val processingString = "processing"
  private val successString = "success"
  private val failureString = "failure"

  case object Initialised extends Status {
    override def toString: String = initialisedString
  }

  case object Processing extends Status {
    override def toString: String = processingString
  }

  case object Completed extends Status {
    override def toString: String = successString
  }

  case object Failed extends Status {
    override def toString: String = failureString
  }

}
