package uk.ac.wellcome.platform.archive.archivist

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.typesafe.config.ConfigFactory
import grizzled.slf4j.Logging
import uk.ac.wellcome.platform.archive.archivist.config.BagUploaderConfigBuilder
import uk.ac.wellcome.platform.archive.common.config.builders.{
  AkkaBuilder,
  MessagingBuilder,
  S3Builder,
  SNSBuilder
}
import uk.ac.wellcome.platform.archive.common.models.NotificationMessage

import scala.concurrent.{Await, ExecutionContext}
import scala.concurrent.duration._

object Main extends App with Logging {
  val config = ConfigFactory.load()

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

  try {
    info(s"Starting worker.")

    val app = archivist.run()

    Await.result(app, Duration.Inf)
  } catch {
    case e: Throwable =>
      error("Fatal error:", e)
  } finally {
    info(s"Terminating worker.")
  }
}
