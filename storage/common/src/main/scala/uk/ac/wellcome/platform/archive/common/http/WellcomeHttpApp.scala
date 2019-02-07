package uk.ac.wellcome.platform.archive.common.http

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.{ExceptionHandler, RejectionHandler}
import akka.stream.Materializer
import grizzled.slf4j.Logging
import uk.ac.wellcome.platform.archive.common.config.models.HTTPServerConfig

import scala.concurrent.{ExecutionContext, Future}

object WellcomeHttpApp extends Logging {
  def run(
    httpServerConfig: HTTPServerConfig,
    router: BaseRouter)(
    implicit actorSystem: ActorSystem,
    materializer: Materializer,
    ec: ExecutionContext): Future[Http.HttpTerminated] = {
    implicit val rejectionHandler: RejectionHandler = router.rejectionHandler
    implicit val exceptionHandler: ExceptionHandler = router.exceptionHandler

    val bindingFuture: Future[Http.ServerBinding] = Http()
      .bindAndHandle(
        handler = router.routes,
        interface = httpServerConfig.host,
        port = httpServerConfig.port
      )

    bindingFuture
      .map(b => {
        info(s"Listening on ${httpServerConfig.host}:${httpServerConfig.port}")
        b
      })
      .flatMap { _.whenTerminated }
  }
}
