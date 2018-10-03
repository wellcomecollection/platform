package uk.ac.wellcome.platform.archive.notifier.flows

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.stream.scaladsl.Flow
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.platform.archive.common.progress.models.Progress
import uk.ac.wellcome.platform.archive.notifier.CallbackNotification
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
        case CallbackNotification(id, callbackUrl, progress) =>
          (createHttpRequest(progress, callbackUrl), id)
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
    Http().superPool[String]()

  private def createHttpRequest(progress: Progress,
                                callbackUri: String): HttpRequest = {

    // Making a `.get` here!
    val entity = HttpEntity(
      ContentTypes.`application/json`,
      toJson(progress).get
    )

    HttpRequest(
      method = HttpMethods.POST,
      uri = Uri(callbackUri.toString),
      entity = entity
    )
  }
}