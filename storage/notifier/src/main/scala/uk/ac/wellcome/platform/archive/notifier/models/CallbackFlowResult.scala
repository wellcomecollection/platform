package uk.ac.wellcome.platform.archive.notifier.models

import java.util.UUID

import akka.http.scaladsl.model.HttpResponse

import scala.util.Try

case class CallbackFlowResult(
  id: UUID,
  httpResponse: Try[HttpResponse]
)
