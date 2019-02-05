package uk.ac.wellcome.platform.archive.common.progress.models

import java.time.Instant
import java.util.UUID

import uk.ac.wellcome.platform.archive.common.models.bagit.BagId

case class Progress(id: UUID,
                    sourceLocation: StorageLocation,
                    space: Namespace,
                    callback: Option[Callback],
                    status: Progress.Status,
                    bag: Option[BagId],
                    createdDate: Instant,
                    lastModifiedDate: Instant,
                    events: Seq[ProgressEvent])

case object Progress {
  def apply(id: UUID,
            sourceLocation: StorageLocation,
            space: Namespace,
            callback: Option[Callback] = None,
            status: Progress.Status = Progress.Accepted,
            bag: Option[BagId] = None,
            createdDate: Instant = Instant.now(),
            events: Seq[ProgressEvent] = Seq.empty): Progress = {
    Progress(
      id,
      sourceLocation,
      space,
      callback,
      status,
      bag,
      createdDate,
      createdDate,
      events)
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

case class BagIngest(bagIdIndex: String, id: UUID, createdDate: Instant)
