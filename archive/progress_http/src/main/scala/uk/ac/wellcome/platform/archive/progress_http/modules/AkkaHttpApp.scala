package uk.ac.wellcome.platform.archive.progress_http.modules

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import com.google.inject.Injector
import grizzled.slf4j.Logging
import uk.ac.wellcome.platform.archive.common.modules.HttpServerConfig
import uk.ac.wellcome.platform.archive.progress_http.Router

import scala.concurrent.{ExecutionContext, Future}

trait AkkaHttpApp extends Logging {
  val injector: Injector

  def run() = {
    val router = injector.getInstance(classOf[Router])
    val config = injector.getInstance(classOf[HttpServerConfig])

    implicit val sys = injector.getInstance(classOf[ActorSystem])
    implicit val mat = injector.getInstance(classOf[ActorMaterializer])
    implicit val ctx = injector.getInstance(classOf[ExecutionContext])

    val bindingFuture: Future[Http.ServerBinding] = Http()
      .bindAndHandle(router.routes, config.host, config.port)

    val startMessage =
      s"Listening on ${config.host}:${config.port}"

    bindingFuture
      .map(b => {
        info(startMessage)

        b
      })
      .flatMap(_.whenTerminated)
  }
}
