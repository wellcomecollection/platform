package uk.ac.wellcome.platform.archive.registrar.http

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.typesafe.config.ConfigFactory
import grizzled.slf4j.Logging
import uk.ac.wellcome.config.core.builders.AkkaBuilder
import uk.ac.wellcome.config.storage.builders.VHSBuilder
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.platform.archive.common.config.builders._
import uk.ac.wellcome.platform.archive.registrar.common.models.StorageManifest
import uk.ac.wellcome.storage.vhs.EmptyMetadata

import scala.concurrent.{Await, ExecutionContext}
import scala.concurrent.duration.Duration

object Main extends App with Logging {
  val config = ConfigFactory.load()

  implicit val actorSystem: ActorSystem = AkkaBuilder.buildActorSystem()
  implicit val materializer: ActorMaterializer =
    AkkaBuilder.buildActorMaterializer()
  implicit val executionContext: ExecutionContext =
    AkkaBuilder.buildExecutionContext()

  val vhs = VHSBuilder.buildVHS[StorageManifest, EmptyMetadata](config)

  val registrarHTTP = new RegistrarHTTP(
    vhs = vhs,
    httpServerConfig = HTTPServerBuilder.buildHTTPServerConfig(config),
    contextURL = HTTPServerBuilder.buildContextURL(config)
  )

  try {
    info(s"Starting service.")

    val app = registrarHTTP.run()

    Await.result(app, Duration.Inf)
  } catch {
    case e: Throwable =>
      error("Fatal error:", e)
  } finally {
    info(s"Terminating service.")
  }
}
