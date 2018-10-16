package uk.ac.wellcome.platform.archive.notifier.flows

import akka.NotUsed
import akka.http.scaladsl.model.{HttpResponse, StatusCodes}
import akka.stream.scaladsl.Flow
import grizzled.slf4j.Logging
import uk.ac.wellcome.platform.archive.common.progress.models.progress.Callback.{
  Failed,
  Succeeded
}
import uk.ac.wellcome.platform.archive.common.progress.models.progress._
import uk.ac.wellcome.platform.archive.notifier.models.CallbackFlowResult

import scala.util.{Failure, Success}

object PrepareNotificationFlow extends Logging {

  def apply(): Flow[CallbackFlowResult, ProgressUpdate, NotUsed] = {
    Flow[CallbackFlowResult].map {
      case CallbackFlowResult(
          id,
          Some(Success(HttpResponse(StatusCodes.OK, _, _, _)))) =>
        info(s"Callback fulfilled for: $id")

        ProgressCallbackStatusUpdate(
          id,
          Succeeded,
          List(ProgressEvent("Callback fulfilled.")),
        )
      case CallbackFlowResult(
          id,
          Some(Success(HttpResponse(status, _, _, _)))) =>
        info(s"Callback failed for: $id, got $status!")

        ProgressCallbackStatusUpdate(
          id,
          Failed,
          List(ProgressEvent(s"Callback failed for: $id, got $status!"))
        )
      case CallbackFlowResult(id, Some(Failure(e))) =>
        error(s"Callback failed for: $id", e)

        ProgressCallbackStatusUpdate(
          id,
          Failed,
          List(ProgressEvent(s"Callback failed for: $id (${e.getMessage})"))
        )
    }
  }
}
