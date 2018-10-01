package uk.ac.wellcome.platform.archive.call_backerei.flows

import akka.NotUsed
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.stream.FlowShape
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, Keep, Merge}
import uk.ac.wellcome.json.JsonUtil.{toJson, _}
import uk.ac.wellcome.platform.archive.common.progress.models.Progress

import scala.util.Try

case class CallbackPayload(id: String)

case class CallbackResult(
  progress: Progress,
  httpResponse: Option[Try[HttpResponse]]
)

object CallbackFlow {
  def apply()(implicit actorSystem: ActorSystem): Flow[Progress, CallbackResult, NotUsed] = {
    val http = Http().superPool[Progress]()

    Flow.fromGraph(
      GraphDSL.create(Broadcast[Progress](2), Merge[CallbackResult](2))(Keep.none) {
        implicit builder =>
          import GraphDSL.Implicits._
          (broadcast, merge) =>
          {
            val withCallbackUrlFlow = Flow[Progress]
              .collect {
                case progress@Progress(id, _, Some(callbackUrl), _, _, _, _) =>
                  (createHttpRequest(id, callbackUrl), progress)
              }
              .via(http)
              .map {
                case (tryHttpResponse, progress) =>
                  CallbackResult(
                    progress = progress,
                    httpResponse = Some(tryHttpResponse)
                  )
              }

            val withoutCallbackUrlFlow = Flow[Progress]
              .filter { _.callbackUrl.isEmpty }
              .map { progress: Progress =>
                CallbackResult(progress = progress, httpResponse = None)
              }

            val withCallbackUrl = builder.add(withCallbackUrlFlow)
            val withoutCallbackUrl = builder.add(withoutCallbackUrlFlow)

            broadcast.out(0) ~> withCallbackUrl ~> merge.in(0)
            broadcast.out(1) ~> withoutCallbackUrl ~> merge.in(1)

            FlowShape(broadcast.in, merge.out)
          }
      })
  }

  private def createHttpRequest(id: String, callbackUri: String): HttpRequest = {
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
