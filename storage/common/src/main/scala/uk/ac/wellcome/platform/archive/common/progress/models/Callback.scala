package uk.ac.wellcome.platform.archive.common.progress.models

import java.net.URI

import uk.ac.wellcome.platform.archive.common.progress.models.Callback.Pending

case class Callback(uri: URI, status: Callback.CallbackStatus = Pending)

case object Callback {
  sealed trait CallbackStatus

  def apply(callbackUri: URI): Callback = {
    Callback(uri = callbackUri)
  }

  def apply(callbackUri: Option[URI]): Option[Callback] = {
    callbackUri.map(Callback(_))
  }

  private val processingString = "processing"
  private val succeededString = "succeeded"
  private val failedString = "failed"

  case object Pending extends CallbackStatus {
    override def toString: String = processingString
  }

  case object Succeeded extends CallbackStatus {
    override def toString: String = succeededString
  }

  case object Failed extends CallbackStatus {
    override def toString: String = failedString
  }
}
