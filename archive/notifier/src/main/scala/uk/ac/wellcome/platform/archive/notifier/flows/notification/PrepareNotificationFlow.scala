package uk.ac.wellcome.platform.archive.notifier.flows.notification

import akka.NotUsed
import akka.http.scaladsl.model.{HttpResponse, StatusCodes}
import akka.stream.scaladsl.Flow
import grizzled.slf4j.Logging
import uk.ac.wellcome.platform.archive.common.progress.models.{Progress, ProgressEvent, ProgressUpdate}
import uk.ac.wellcome.platform.archive.notifier.models.CallbackFlowResult

import scala.util.{Failure, Success}

object PrepareNotificationFlow extends Logging{

  val noCallBackEvent = ProgressEvent(
    "No callback provided."
  )

  val callBackSuccessEvent = ProgressEvent(
    "Callback fulfilled."
  )

  val callBackFailureEvent = ProgressEvent(
    "Callback could not be fulfilled!"
  )

  def apply(): Flow[CallbackFlowResult, ProgressUpdate, NotUsed] = {
    Flow[CallbackFlowResult].map {
      case CallbackFlowResult(progress, None) => {
        debug(s"No callback required for $progress")

        ProgressUpdate(progress.id, noCallBackEvent,
          Progress.CompletedNoCallbackProvided)
      }
      case CallbackFlowResult(progress, Some(Success(HttpResponse(StatusCodes.OK,_,_,_)))) => {
        info(s"Callback fulfilled for: $progress")

        ProgressUpdate(progress.id, callBackSuccessEvent,
          Progress.CompletedCallbackSucceeded)
      }
      case CallbackFlowResult(progress, Some(Success(HttpResponse(status,_,_,_)))) => {
        info(s"Callback failed for: $progress, got $status!")

        ProgressUpdate(progress.id, callBackFailureEvent,
          Progress.CompletedCallbackFailed)
      }
      case CallbackFlowResult(progress, Some(Failure(e))) => {
        error(s"Callback failed for: $progress", e)

        ProgressUpdate(progress.id, callBackFailureEvent,
          Progress.CompletedCallbackFailed)
      }
    }
    .map{ r =>
      debug(s"PrepareNotificationFlow: $r")

      r
    }
  }

}

