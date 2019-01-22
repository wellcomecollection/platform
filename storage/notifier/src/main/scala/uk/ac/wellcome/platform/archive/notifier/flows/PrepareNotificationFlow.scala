package uk.ac.wellcome.platform.archive.notifier.flows

import akka.NotUsed
import akka.http.scaladsl.model.HttpResponse
import akka.stream.scaladsl.Flow
import grizzled.slf4j.Logging
import uk.ac.wellcome.platform.archive.common.progress.models.Callback.{
  Failed,
  Succeeded
}
import uk.ac.wellcome.platform.archive.common.progress.models.{
  ProgressCallbackStatusUpdate,
  ProgressEvent,
  ProgressUpdate
}
import uk.ac.wellcome.platform.archive.notifier.models.CallbackFlowResult

import scala.util.{Failure, Success}

object PrepareNotificationFlow extends Logging {
  def apply(): Flow[CallbackFlowResult, ProgressUpdate, NotUsed] = {
    Flow[CallbackFlowResult].map {
      case CallbackFlowResult(id, response) =>
        response match {
          case Success(HttpResponse(status, _, _, _)) =>
            if (status.isSuccess()) {
              debug(s"Callback fulfilled for: $id")

              ProgressCallbackStatusUpdate(
                id = id,
                callbackStatus = Succeeded,
                events = List(ProgressEvent("Callback fulfilled."))
              )
            } else {
              debug(s"Callback failed for: $id, got $status!")

              ProgressCallbackStatusUpdate(
                id = id,
                callbackStatus = Failed,
                events =
                  List(ProgressEvent(s"Callback failed for: $id, got $status!"))
              )
            }
          case Failure(e) =>
            error(s"Callback failed for: $id", e)

            ProgressCallbackStatusUpdate(
              id = id,
              callbackStatus = Failed,
              events = List(
                ProgressEvent(s"Callback failed for: $id (${e.getMessage})"))
            )
        }
    }
  }
}
