package uk.ac.wellcome.platform.archive.progress_http

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.typesafe.config.ConfigFactory
import grizzled.slf4j.Logging
import uk.ac.wellcome.platform.archive.common.config.builders.{
  AkkaBuilder,
  DynamoBuilder,
  HTTPServerBuilder,
  SNSBuilder
}

import scala.concurrent.{Await, ExecutionContext}
import scala.concurrent.duration._

object Main extends App with Logging {
  val config = ConfigFactory.load()

  implicit val actorSystem: ActorSystem = AkkaBuilder.buildActorSystem()
  implicit val materializer: ActorMaterializer =
    AkkaBuilder.buildActorMaterializer()
  implicit val executionContext: ExecutionContext =
    AkkaBuilder.buildExecutionContext()

  val progressHTTP = new ProgressHTTP(
    dynamoClient = DynamoBuilder.buildDynamoClient(config),
    dynamoConfig = DynamoBuilder.buildDynamoConfig(config),
    snsWriter = SNSBuilder.buildSNSWriter(config),
    httpServerConfig = HTTPServerBuilder.buildHTTPServerConfig(config),
    contextURL = HTTPServerBuilder.buildContextURL(config)
  )

  try {
    info(s"Starting service.")

    val app = progressHTTP.run()

    Await.result(app, Duration.Inf)
  } catch {
    case e: Throwable =>
      error("Fatal error:", e)
  } finally {
    info(s"Terminating service.")
  }
}
