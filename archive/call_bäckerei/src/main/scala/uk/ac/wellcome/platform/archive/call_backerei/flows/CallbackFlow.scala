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

/** Represents the payload sent to a callback URL.
  *
  * @param id ID of the ingest request that we're sending a callback for.
  */
case class CallbackPayload(id: String)

case class CallbackFlowResult(
  progress: Progress,
  httpResponse: Option[Try[HttpResponse]]
)

/** This flow receives a [[Progress]] instance, as received from
  * an SQS queue, and makes the callback request (if necessary).
  *
  * After this flow has done, we need to know two things:
  *   - The original Progress object
  *   - Whether the callback succeeded (if necessary) -- we record
  *     the callback result.
  *
  */
object CallbackFlow {
  def apply()(implicit actorSystem: ActorSystem)
    : Flow[Progress, CallbackFlowResult, NotUsed] = {

    // This flow handles the case where there is a callback URL on
    // the progress object.
    //
    // The HTTP pool takes an tuple (HttpRequest, Progress), and returns
    // a (Try[HttpResponse], Progress) tuple -- preserving the original
    // progress as context.
    //
    val http = Http().superPool[Progress]()
    val withCallbackUrlFlow = Flow[Progress]
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

    // This flow handles the case where there isn't a callback URL on
    // the progress object -- there's nothing to do, just prepare the result.
    //
    val withoutCallbackUrlFlow = Flow[Progress]
      .filter { _.callbackUrl.isEmpty }
      .map { progress: Progress =>
        CallbackFlowResult(progress = progress, httpResponse = None)
      }

    // This creates a custom Akka graph stage, and splits Progress objects
    // based on whether they have a callback URL.
    //
    //                              Broadcast                     Merge
    //
    //                  +-----> withCallbackUrlFlow     ~~~> [CallbackFlowResult]
    //                  |       (has a callback URL)
    //    [Progress] ---+
    //                  |
    //                  +-----> withoutCallbackUrlFlow  ~~~> [CallbackFlowResult]
    //                          (no callback URL)
    //
    // How it works:
    //
    //  1.  Every instance of Progress sent to the original flow is duplicated,
    //      and sent to both `withCallbackUrlFlow` and `withoutCallbackUrlFlow`.
    //      (This is the `Broadcast` component.)
    //
    //  2.  Each flow filters out half the messages, so each Progress is handled
    //      by exactly once flow.
    //
    //  3.  Each flow processes Progress messages and turns them into instances of
    //      CallbackFlowResult.
    //
    //  4.  The result is two flows that both take [Progress ~> CallbackFlowResult].
    //      (This is the `Merge` component.)
    //
    Flow.fromGraph(
      GraphDSL.create(Broadcast[Progress](2), Merge[CallbackFlowResult](2))(
        Keep.none) { implicit builder =>
        import GraphDSL.Implicits._
        (broadcast, merge) =>
          {
            val withCallbackUrl = builder.add(withCallbackUrlFlow)
            val withoutCallbackUrl = builder.add(withoutCallbackUrlFlow)

            broadcast.out(0) ~> withCallbackUrl ~> merge.in(0)
            broadcast.out(1) ~> withoutCallbackUrl ~> merge.in(1)

            FlowShape(broadcast.in, merge.out)
          }
      })
  }

  private def createHttpRequest(id: String,
                                callbackUri: String): HttpRequest = {
    val contentJson = ContentTypes.`application/json`
    val jsonBody = toJson(CallbackPayload(id)).get // TODO: Make this not be a ".get"
    val entity = HttpEntity(contentJson, jsonBody)

    HttpRequest(
      method = HttpMethods.POST,
      uri = Uri(callbackUri.toString),
      entity = entity
    )
  }
}
