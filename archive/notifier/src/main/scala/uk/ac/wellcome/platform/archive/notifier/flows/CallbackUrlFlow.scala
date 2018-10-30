package uk.ac.wellcome.platform.archive.notifier.flows

import java.net.URI
import java.util.UUID

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.stream.scaladsl.Flow
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.platform.archive.common.models.{CallbackNotification, ResponseDisplayIngest}
import uk.ac.wellcome.platform.archive.common.progress.models.Progress
import uk.ac.wellcome.platform.archive.notifier.models.CallbackFlowResult

object CallbackUrlFlow {

  // This flow handles the case where there is a callback URL on
  // the progress object.
  //
  // The HTTP pool takes an tuple (HttpRequest, String), and returns
  // a (Try[HttpResponse], String) tuple -- preserving the original
  // progress as context.
  //
  def apply()(implicit actorSystem: ActorSystem) =
    Flow[CallbackNotification]
      .collect {
        case CallbackNotification(id, callbackUri, progress) =>
          (createHttpRequest(progress, callbackUri), id)
      }
      .via(http)
      .map {
        case (tryHttpResponse, id) =>
          CallbackFlowResult(
            id = id,
            httpResponse = Some(tryHttpResponse)
          )
      }

  private def http(implicit actorSystem: ActorSystem) =
    Http().superPool[UUID]()

  private def createHttpRequest(progress: Progress,
                                callbackUri: URI): HttpRequest = {
    val entity = HttpEntity(
      ContentTypes.`application/json`,
      toJson(ResponseDisplayIngest(progress)).get
    )

    HttpRequest(
      method = HttpMethods.POST,
      uri = callbackUri.toString,
      entity = entity
    )
  }
}
