package uk.ac.wellcome.platform.archive.registrar.flows

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.stream.scaladsl.{Flow, Source}
import grizzled.slf4j.Logging
import uk.ac.wellcome.json.JsonUtil.{toJson, _}
import uk.ac.wellcome.platform.archive.registrar.models.RegisterRequestContext

object CallbackFlow extends Logging {

  def apply()(implicit actorSystem: ActorSystem) = {
    val http = Http().superPool[RegisterRequestContext]()

    Flow[(_, RegisterRequestContext)]
      .flatMapConcat({
        case (in, context) =>
          Source
            .single(context)
            .map(createRequest)
            .via(http)
      })
  }

  def createRequest(context: RegisterRequestContext) = {
    val callbackUri = context.callbackUrl.get
    val contentJson = ContentTypes.`application/json`
    val jsonBody = toJson(CallbackPayload(context.requestId.toString)).get
    val entity = HttpEntity(contentJson, jsonBody)

    (HttpRequest(
      method = HttpMethods.POST,
      uri = Uri(callbackUri.toString),
      entity = entity
    ), context)
  }
}

case class CallbackPayload(id: String)


