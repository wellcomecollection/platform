package uk.ac.wellcome.platform.archive.common.progress.models

import java.time.Instant
import java.util.UUID

import uk.ac.wellcome.platform.archive.common.json.URIConverters
import uk.ac.wellcome.platform.archive.common.models.{BagId, Namespace}

case class Progress(
  id: UUID,
  sourceLocation: StorageLocation,
  space: Namespace,
  callback: Option[Callback],
  status: Progress.Status,
  bag: Option[BagId],
  createdDate: Instant,
  lastModifiedDate: Instant,
  events: Seq[ProgressEvent]
)

case object Progress extends URIConverters {

  // The important bit of this apply() method is that it only creates the
  // Instant for the created/modified date once, so it's always the same.
  //
  // If you set `Instant.now` as the default on the case class, it gets
  // created twice -- and if you're unlucky, they'll be different.
  def apply(
    id: UUID,
    sourceLocation: StorageLocation,
    space: Namespace,
    callback: Option[Callback] = None,
    status: Progress.Status = Progress.Accepted,
    bag: Option[BagId] = None,
    events: Seq[ProgressEvent] = Seq.empty
  ): Progress = {
    val now = Instant.now

    Progress(
      id = id,
      sourceLocation = sourceLocation,
      space = space,
      callback = callback,
      status = status,
      bag = bag,
      createdDate = now,
      lastModifiedDate = now,
      events = events
    )
  }

  sealed trait Status

  private val acceptedString = "accepted"
  private val processingString = "processing"
  private val succeededString = "succeeded"
  private val failedString = "failed"

  case object Accepted extends Status {
    override def toString: String = acceptedString
  }

  case object Processing extends Status {
    override def toString: String = processingString
  }

  case object Completed extends Status {
    override def toString: String = succeededString
  }

  case object Failed extends Status {
    override def toString: String = failedString
  }

}
