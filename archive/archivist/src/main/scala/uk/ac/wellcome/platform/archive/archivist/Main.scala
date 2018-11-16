package uk.ac.wellcome.platform.archive.archivist

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.typesafe.config.{Config, ConfigFactory}
import uk.ac.wellcome.WellcomeApp
import uk.ac.wellcome.config.core.builders.AkkaBuilder
import uk.ac.wellcome.config.messaging.builders.SNSBuilder
import uk.ac.wellcome.config.storage.builders.S3Builder
import uk.ac.wellcome.platform.archive.archivist.config.BagUploaderConfigBuilder
import uk.ac.wellcome.platform.archive.common.config.builders.MessagingBuilder
import uk.ac.wellcome.platform.archive.common.models.NotificationMessage

import scala.concurrent.ExecutionContext

object Main extends WellcomeApp {
  val config: Config = ConfigFactory.load()

  implicit val actorSystem: ActorSystem = AkkaBuilder.buildActorSystem()
  implicit val materializer: ActorMaterializer =
    AkkaBuilder.buildActorMaterializer()
  implicit val executionContext: ExecutionContext =
    AkkaBuilder.buildExecutionContext()

  val archivist = new Archivist(
    s3Client = S3Builder.buildS3Client(config),
    snsClient = SNSBuilder.buildSNSClient(config),
    messageStream =
      MessagingBuilder.buildMessageStream[NotificationMessage, Unit](config),
    bagUploaderConfig = BagUploaderConfigBuilder.buildBagUploaderConfig(config),
    snsRegistrarConfig =
      SNSBuilder.buildSNSConfig(config, namespace = "registrar"),
    snsProgressConfig =
      SNSBuilder.buildSNSConfig(config, namespace = "progress")
  )

  run(archivist)
}
