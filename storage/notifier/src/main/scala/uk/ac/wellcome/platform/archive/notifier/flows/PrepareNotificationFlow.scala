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
      case CallbackFlowResult(
          id,
          Some(Success(HttpResponse(status, _, _, _)))) =>
        if (status.isSuccess()) {
          info(s"Callback fulfilled for: $id")

          ProgressCallbackStatusUpdate(
            id,
            Succeeded,
            List(ProgressEvent("Callback fulfilled.")),
          )
        } else {
          info(s"Callback failed for: $id, got $status!")

          ProgressCallbackStatusUpdate(
            id,
            Failed,
            List(ProgressEvent(s"Callback failed for: $id, got $status!"))
          )
        }
      case CallbackFlowResult(id, Some(Failure(e))) =>
        error(s"Callback failed for: $id", e)

        ProgressCallbackStatusUpdate(
          id,
          Failed,
          List(ProgressEvent(s"Callback failed for: $id (${e.getMessage})"))
        )
      case CallbackFlowResult(id, result) =>
        error(s"Unexpected callback failure for: $id got $result")

        ProgressCallbackStatusUpdate(
          id,
          Failed,
          List(ProgressEvent(s"Unexpected callback failure for: $id"))
        )
    }
  }
}
