package uk.ac.wellcome.platform.archive.common.progress.models

import java.net.URI

import uk.ac.wellcome.platform.archive.common.json.URIConverters
import uk.ac.wellcome.platform.archive.common.progress.models.Callback.Pending

case class Callback(uri: URI, status: Callback.CallbackStatus = Pending)

case object Callback extends URIConverters {
  sealed trait CallbackStatus

  def apply(callbackUri: URI): Callback = {
    Callback(uri = callbackUri)
  }

  def apply(callbackUri: Option[URI]): Option[Callback] = {
    callbackUri.map(Callback(_))
  }

  val processingString = "processing"
  val successString = "success"
  val failureString = "failure"

  case object Pending extends CallbackStatus {
    override def toString: String = processingString
  }

  case object Succeeded extends CallbackStatus {
    override def toString: String = successString
  }

  case object Failed extends CallbackStatus {
    override def toString: String = failureString
  }

  def parseStatus(string: String): CallbackStatus = {
    string match {
      case `processingString`   => Pending
      case `successString` => Succeeded
      case `failureString`    => Failed
    }
  }
}
