package uk.ac.wellcome.platform.archive.registrar.http.modules

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import com.google.inject.Injector
import grizzled.slf4j.Logging
import uk.ac.wellcome.platform.archive.common.config.builders.AkkaBuilder
import uk.ac.wellcome.platform.archive.common.config.models.OldHttpServerConfig
import uk.ac.wellcome.platform.archive.registrar.http.Router

import scala.concurrent.{ExecutionContext, Future}

trait AkkaHttpApp extends Logging {
  val injector: Injector

  def run(): Future[Http.HttpTerminated] = {
    val router = injector.getInstance(classOf[Router])
    val config = injector.getInstance(classOf[OldHttpServerConfig])

    implicit val actorSystem: ActorSystem = AkkaBuilder.buildActorSystem()
    implicit val materializer: ActorMaterializer = ActorMaterializer()
    implicit val executionContext: ExecutionContext = actorSystem.dispatcher

    val bindingFuture: Future[Http.ServerBinding] = Http()
      .bindAndHandle(router.routes, config.host, config.port)

    val startMessage =


    bindingFuture
      .map(b => {
        info(startMessage)

        b
      })
      .flatMap(_.whenTerminated)
  }
}
