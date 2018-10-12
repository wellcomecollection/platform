package uk.ac.wellcome.platform.archive.notifier.flows

import akka.NotUsed
import akka.http.scaladsl.model.{HttpResponse, StatusCodes}
import akka.stream.scaladsl.Flow
import grizzled.slf4j.Logging
import uk.ac.wellcome.platform.archive.common.progress.models.{
  Progress,
  ProgressEvent,
  ProgressUpdate
}
import uk.ac.wellcome.platform.archive.notifier.models.CallbackFlowResult

import scala.util.{Failure, Success}

object PrepareNotificationFlow extends Logging {

  def apply(): Flow[CallbackFlowResult, ProgressUpdate, NotUsed] = {
    Flow[CallbackFlowResult].map {
      case CallbackFlowResult(
          id,
          Some(Success(HttpResponse(StatusCodes.OK, _, _, _)))) =>
        info(s"Callback fulfilled for: $id")

        ProgressUpdate(
          id,
          List(ProgressEvent("Callback fulfilled.")),
          Progress.CompletedCallbackSucceeded)
      case CallbackFlowResult(
          id,
          Some(Success(HttpResponse(status, _, _, _)))) =>
        info(s"Callback failed for: $id, got $status!")

        ProgressUpdate(
          id,
          List(ProgressEvent(s"Callback failed for: $id, got $status!")),
          Progress.CompletedCallbackFailed
        )
      case CallbackFlowResult(id, Some(Failure(e))) =>
        error(s"Callback failed for: $id", e)

        ProgressUpdate(
          id,
          List(ProgressEvent(s"Callback failed for: $id (${e.getMessage})")),
          Progress.CompletedCallbackFailed
        )
    }
  }
}
