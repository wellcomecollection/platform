package uk.ac.wellcome.platform.archive.notifier.models

import akka.http.scaladsl.model.HttpResponse

import scala.util.Try

case class CallbackFlowResult(
  id: String,
  httpResponse: Option[Try[HttpResponse]]
)
