package uk.ac.wellcome.platform.archive.common.progress.models.progress

import java.net.URI

import uk.ac.wellcome.platform.archive.common.progress.models.progress.Callback.Pending

import uk.ac.wellcome.platform.archive.common.json.URIConverters

case class Callback(uri: URI, status: Callback.CallbackStatus = Pending)

case object Callback extends URIConverters {
  sealed trait CallbackStatus

  def apply(callbackUri: URI): Callback = {
    Callback(uri = callbackUri)
  }

  def apply(callbackUri: Option[URI]): Option[Callback] = {
    callbackUri.map(Callback(_))
  }

  val pendingString   = "pending"
  val succeededString = "succeeded"
  val failedString    = "failed"

  case object Pending extends CallbackStatus {
    override def toString: String = pendingString
  }

  case object Succeeded extends CallbackStatus {
    override def toString: String = succeededString
  }

  case object Failed extends CallbackStatus {
    override def toString: String = failedString
  }

  def parseStatus(string: String): CallbackStatus = {
    string match {
      case `pendingString`   => Pending
      case `succeededString` => Succeeded
      case `failedString`    => Failed
    }
  }
}

