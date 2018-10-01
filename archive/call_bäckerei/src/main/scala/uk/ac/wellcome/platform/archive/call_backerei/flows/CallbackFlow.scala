package uk.ac.wellcome.platform.archive.call_backerei.flows

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.stream.scaladsl.Flow
import grizzled.slf4j.Logging
import uk.ac.wellcome.json.JsonUtil.{toJson, _}
import uk.ac.wellcome.platform.archive.common.progress.models.Progress

object CallbackFlow extends Logging {

  def apply()(implicit actorSystem: ActorSystem) = {
    val http = Http().superPool[Progress]()

    Flow[Progress]
      .filter { _.callbackUrl.isDefined }
      .collect {
        case progress @ Progress(id, _, Some(callbackUri), _, _, _, _) =>
          val httpRequest = createHttpRequest(id, callbackUri)
          (httpRequest, progress)
      }
      .via(http)
  }

  def createHttpRequest(id: String, callbackUri: String): HttpRequest = {
    val contentJson = ContentTypes.`application/json`
    val jsonBody = toJson(CallbackPayload(id)).get  // TODO: Make this not be a ".get"
    val entity = HttpEntity(contentJson, jsonBody)

    HttpRequest(
      method = HttpMethods.POST,
      uri = Uri(callbackUri.toString),
      entity = entity
    )
  }
}

case class CallbackPayload(id: String)
