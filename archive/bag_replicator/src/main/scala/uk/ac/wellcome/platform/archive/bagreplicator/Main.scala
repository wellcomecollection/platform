package uk.ac.wellcome.platform.archive.bagreplicator

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.typesafe.config.ConfigFactory
import grizzled.slf4j.Logging
import uk.ac.wellcome.config.core.builders.AkkaBuilder
import uk.ac.wellcome.config.messaging.builders.SNSBuilder
import uk.ac.wellcome.config.storage.builders.S3Builder
import uk.ac.wellcome.platform.archive.bagreplicator.config.BagReplicatorConfig
import uk.ac.wellcome.platform.archive.common.config.builders.MessagingBuilder
import uk.ac.wellcome.platform.archive.common.models.NotificationMessage

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext}

object Main extends App with Logging {
  val config = ConfigFactory.load()

  implicit val actorSystem: ActorSystem =
    AkkaBuilder.buildActorSystem()
  implicit val materializer: ActorMaterializer =
    AkkaBuilder.buildActorMaterializer()
  implicit val executionContext: ExecutionContext =
    AkkaBuilder.buildExecutionContext()

  val bagReplicator = new BagReplicator(
    s3Client = S3Builder.buildS3Client(config),
    snsClient = SNSBuilder.buildSNSClient(config),
    messageStream =
      MessagingBuilder.buildMessageStream[NotificationMessage, Unit](config),
    bagReplicatorConfig = BagReplicatorConfig.buildBagUploaderConfig(config),
    snsProgressConfig =
      SNSBuilder.buildSNSConfig(config, namespace = "progress")
  )

  try {
    info(s"Starting worker.")

    val app = bagReplicator.run()

    Await.result(app, Duration.Inf)
  } catch {
    case e: Throwable =>
      error("Fatal error:", e)
  } finally {
    info(s"Terminating worker.")
  }
}
