package uk.ac.wellcome.platform.archive.progress_http.modules

import akka.http.scaladsl.model.HttpMethods.GET
import akka.http.scaladsl.model.{ContentTypes, HttpRequest, HttpResponse, Uri}
import akka.stream.ActorMaterializer
import com.google.inject.{AbstractModule, Inject, Provides}
import uk.ac.wellcome.platform.archive.common.progress.monitor.ProgressMonitor
import uk.ac.wellcome.json.JsonUtil._

object RequestHandlerModule extends AbstractModule {


  @Provides
  def providesRequestHandler(handlerFactory: RequestHandlerFactory):
  HttpRequest => HttpResponse = handlerFactory.create
}

class RequestHandlerFactory @Inject()(monitor: ProgressMonitor)(
  implicit m: ActorMaterializer
) extends DefaultResponses {

  val Json = ContentTypes.`application/json`

  val progress = """/progress/(.+)""".r

  def create: HttpRequest => HttpResponse = {
    case r@HttpRequest(GET, Uri.Path(progress(id)), _, _, _) => {
      monitor.get(id) match {
        case Some(progress) => HttpResponse(entity = toJson(progress).get)
        case None => notFoundResponse(r)
      }
    }

    case r => notFoundResponse(r)
  }
}

trait DefaultResponses {
  def notFoundResponse(r: HttpRequest)(
    implicit m: ActorMaterializer
  ): HttpResponse = {
    r.discardEntityBytes()

    HttpResponse(404, entity = "Unknown resource!")
  }
}