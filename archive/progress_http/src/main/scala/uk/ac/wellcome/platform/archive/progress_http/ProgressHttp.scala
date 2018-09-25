package uk.ac.wellcome.platform.archive.progress_http

import akka.actor.ActorSystem
import akka.event.Logging
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpMethods.GET
import akka.http.scaladsl.model.{HttpRequest, _}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Sink, Source}
import com.google.inject.Injector
import grizzled.slf4j.Logging
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.platform.archive.common.progress.models.Progress
import uk.ac.wellcome.platform.archive.progress_http.models.ProgressHttpConfig
//import uk.ac.wellcome.platform.archive.common.progress.monitor.ProgressMonitor

import scala.concurrent.Future

trait ProgressHttp extends Logging {
  val injector: Injector

  type HttpIn = Http.IncomingConnection
  type ServerBinding = Future[Http.ServerBinding]
  type ServerSource = Source[HttpIn, ServerBinding]
  type ProcessRequest = HttpRequest => HttpResponse

  val Json = ContentTypes.`application/json`

  def fakeResponseBody = {
    toJson(Progress(
      id = "id",
      uploadUrl = "http://www.example.com/",
      callbackUrl = None
    )).get
  }

  def run() = {
    implicit val system =
      injector.getInstance(classOf[ActorSystem])

    implicit val config =
      injector.getInstance(classOf[ProgressHttpConfig])

    implicit val materializer = ActorMaterializer()

    def notFoundResponse(r: HttpRequest) = {
      r.discardEntityBytes()
      HttpResponse(404, entity = "Unknown resource!")
    }

    def requestHandler: ProcessRequest = {
      case HttpRequest(GET, Uri.Path("/progress/id"), _, _, _) =>
        HttpResponse(entity = fakeResponseBody)

      case request => notFoundResponse(request)
    }

    //  val progressMonitor = injector
    //    .getInstance(classOf[ProgressMonitor])

    val serverSource = Http().bind(
      interface = "localhost",
      port = config.port
    )

    val serverSink = Sink.foreach[HttpIn] {
      _.handleWith {
        Flow[HttpRequest].map(requestHandler)
      }
    }

    serverSource.runWith(serverSink)
  }
}
