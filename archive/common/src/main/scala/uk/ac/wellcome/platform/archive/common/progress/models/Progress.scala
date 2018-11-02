package uk.ac.wellcome.platform.archive.common.progress.models

import java.net.URI
import java.time.Instant
import java.util.UUID

import uk.ac.wellcome.platform.archive.common.json.URIConverters
import uk.ac.wellcome.platform.archive.common.models.RequestDisplayIngest

case class Progress(id: UUID,
                    uploadUri: URI,
                    space: Namespace,
                    callback: Option[Callback] = None,
                    status: Progress.Status = Progress.Initialised,
                    resources: Seq[Resource] = Seq.empty,
                    createdDate: Instant = Instant.now,
                    lastModifiedDate: Instant = Instant.now,
                    events: Seq[ProgressEvent] = Seq.empty)

case object Progress extends URIConverters {
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

  def apply(createRequest: RequestDisplayIngest): Progress = {
    Progress(
      id = generateId,
      uploadUri = URI.create(createRequest.uploadUrl),
      callback = Callback(createRequest.callback.map(displayCallback =>
        URI.create(displayCallback.uri))),
      space = Namespace(createRequest.space.id),
      status = Progress.Initialised
    )
  }

  def parseStatus(status: String): Status = {
    status match {
      case `initialisedString` => Initialised
      case `processingString`  => Processing
      case `completedString`   => Completed
      case `failedString`      => Failed
    }
  }

  private def generateId: UUID = UUID.randomUUID
}
