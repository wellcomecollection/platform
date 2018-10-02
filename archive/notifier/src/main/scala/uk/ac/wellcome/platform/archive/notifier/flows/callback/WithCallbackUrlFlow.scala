package uk.ac.wellcome.platform.archive.notifier.flows.callback

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.stream.scaladsl.Flow
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.platform.archive.common.progress.models.Progress
import uk.ac.wellcome.platform.archive.notifier.models.{CallbackFlowResult, CallbackPayload}

object WithCallbackUrlFlow {
  // This flow handles the case where there is a callback URL on
  // the progress object.
  //
  // The HTTP pool takes an tuple (HttpRequest, Progress), and returns
  // a (Try[HttpResponse], Progress) tuple -- preserving the original
  // progress as context.
  //
  def apply()(implicit actorSystem: ActorSystem) = Flow[Progress]
    .collect {
      case progress @ Progress(id, _, Some(callbackUrl), _, _, _, _) =>
        (createHttpRequest(id, callbackUrl), progress)
    }
    .via(http)
    .map {
      case (tryHttpResponse, progress) =>
        CallbackFlowResult(
          progress = progress,
          httpResponse = Some(tryHttpResponse)
        )
    }

  private def http(implicit actorSystem: ActorSystem) =
    Http().superPool[Progress]()

  private def createHttpRequest(id: String,
                                callbackUri: String): HttpRequest = {
    val contentJson = ContentTypes.`application/json`

    // Making a `.get` here!
    val jsonBody = toJson(CallbackPayload(id)).get
    val entity = HttpEntity(contentJson, jsonBody)

    HttpRequest(
      method = HttpMethods.POST,
      uri = Uri(callbackUri.toString),
      entity = entity
    )
  }
}