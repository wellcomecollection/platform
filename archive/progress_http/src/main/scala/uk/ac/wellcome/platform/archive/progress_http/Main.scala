package uk.ac.wellcome.platform.archive.progress_http

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.typesafe.config.{Config, ConfigFactory}
import uk.ac.wellcome.WellcomeApp
import uk.ac.wellcome.config.core.builders.AkkaBuilder
import uk.ac.wellcome.config.messaging.builders.SNSBuilder
import uk.ac.wellcome.config.storage.builders.DynamoBuilder
import uk.ac.wellcome.platform.archive.common.config.builders.HTTPServerBuilder

import scala.concurrent.ExecutionContext

object Main extends WellcomeApp {
  val config: Config = ConfigFactory.load()

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

  run(progressHTTP)
}
