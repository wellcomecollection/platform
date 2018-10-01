package uk.ac.wellcome.platform.archive.progress_http.modules

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import com.google.inject.Injector
import grizzled.slf4j.Logging
import uk.ac.wellcome.platform.archive.progress_http.Router
import uk.ac.wellcome.platform.archive.progress_http.models.HttpServerConfig

trait AkkaHttpApp extends Logging {
  val injector: Injector

  def run() = {
    val router = injector.getInstance(classOf[Router])
    val config = injector.getInstance(classOf[HttpServerConfig])

    implicit val system = injector.getInstance(classOf[ActorSystem])
    implicit val mat = injector.getInstance(classOf[ActorMaterializer])

    Http().bindAndHandle(router.routes, config.host, config.port)
  }
}