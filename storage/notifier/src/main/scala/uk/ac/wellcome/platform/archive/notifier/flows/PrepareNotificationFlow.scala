package uk.ac.wellcome.platform.archive.notifier.flows

import akka.NotUsed
import akka.http.scaladsl.model.HttpResponse
import akka.stream.scaladsl.Flow
import grizzled.slf4j.Logging
import uk.ac.wellcome.platform.archive.common.progress.models.Callback.{
  Failed,
  Succeeded
}
import uk.ac.wellcome.platform.archive.common.progress.models.ProgressUpdate
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

              ProgressUpdate.callback(
                id = id,
                status = Succeeded,
                description = "Callback fulfilled."
              )
            } else {
              debug(s"Callback failed for: $id, got $status!")

              ProgressUpdate.callback(
                id = id,
                status = Failed,
                description = s"Callback failed for: $id, got $status!"
              )
            }
          case Failure(e) =>
            error(s"Callback failed for: $id", e)

            ProgressUpdate.callback(
              id = id,
              status = Failed,
              description = s"Callback failed for: $id (${e.getMessage})"
            )
        }
    }
  }
}
