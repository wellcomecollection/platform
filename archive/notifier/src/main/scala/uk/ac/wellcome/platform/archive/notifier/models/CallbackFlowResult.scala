package uk.ac.wellcome.platform.archive.notifier.models

import akka.http.scaladsl.model.HttpResponse
import uk.ac.wellcome.platform.archive.common.progress.models.Progress

import scala.util.Try

case class CallbackFlowResult(
                               progress: Progress,
                               httpResponse: Option[Try[HttpResponse]]
                             )

