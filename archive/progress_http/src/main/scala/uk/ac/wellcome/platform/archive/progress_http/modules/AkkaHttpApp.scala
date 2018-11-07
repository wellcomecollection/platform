package uk.ac.wellcome.platform.archive.progress_http.modules

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import com.google.inject.Injector
import com.typesafe.config.ConfigFactory
import grizzled.slf4j.Logging
import uk.ac.wellcome.messaging.sns.{SNSMessageWriter, SNSWriter}
import uk.ac.wellcome.platform.archive.common.config.builders.{AkkaBuilder, DynamoBuilder, HTTPServerBuilder, SNSBuilder}
import uk.ac.wellcome.platform.archive.common.config.models.HttpServerConfig
import uk.ac.wellcome.platform.archive.common.progress.monitor.ProgressTracker
import uk.ac.wellcome.platform.archive.progress_http.{ProgressStarter, Router}

import scala.concurrent.{ExecutionContext, Future}

trait AkkaHttpApp extends Logging {
  val injector: Injector

  def run(): Future[Http.HttpTerminated] = {
    val config = ConfigFactory.load()

    val progressTracker = new ProgressTracker(
      dynamoClient = DynamoBuilder.buildDynamoClient(config),
      dynamoConfig = DynamoBuilder.buildDynamoConfig(config)
    )

    val httpServerConfig = HTTPServerBuilder.buildHTTPServerConfig(config)

    val router = new Router(
      monitor = progressTracker,
      progressStarter = new ProgressStarter(
        progressTracker = progressTracker,
        snsWriter = new SNSWriter(
          snsMessageWriter = new SNSMessageWriter(
            snsClient = SNSBuilder.buildSNSClient(config)
          ),
          snsConfig = SNSBuilder.buildSNSConfig(config)
        )
      ),
      config = httpServerConfig
    )

    implicit val actorSystem: ActorSystem = AkkaBuilder.buildActorSystem()
    implicit val materializer: ActorMaterializer = ActorMaterializer()
    implicit val executionContext: ExecutionContext = actorSystem.dispatcher

    val bindingFuture: Future[Http.ServerBinding] = Http()
      .bindAndHandle(router.routes, httpServerConfig.host, httpServerConfig.port)

    val startMessage = s"Listening on ${httpServerConfig.host}:${httpServerConfig.port}"

    bindingFuture
      .map(b => {
        info(startMessage)

        b
      })
      .flatMap(_.whenTerminated)
  }
}
