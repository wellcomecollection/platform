package uk.ac.wellcome.platform.recorder

import akka.actor.ActorSystem
import com.typesafe.config.{Config, ConfigFactory}
import grizzled.slf4j.Logging
import uk.ac.wellcome.config.core.builders.AkkaBuilder
import uk.ac.wellcome.config.messaging.builders.{MessagingBuilder, SNSBuilder}
import uk.ac.wellcome.config.storage.builders.VHSBuilder
import uk.ac.wellcome.models.work.internal.TransformedBaseWork
import uk.ac.wellcome.platform.recorder.services.RecorderWorkerService
import uk.ac.wellcome.storage.vhs.EmptyMetadata

import scala.concurrent.Await
import scala.concurrent.duration.Duration

object Main extends App with Logging {
  val config: Config = ConfigFactory.load()

  implicit val actorSystem: ActorSystem =
    AkkaBuilder.buildActorSystem()

  val workerService = new RecorderWorkerService(
    versionedHybridStore = VHSBuilder.buildVHS[TransformedBaseWork, EmptyMetadata](config),
    messageStream = MessagingBuilder.buildMessageStream[TransformedBaseWork](config),
    snsWriter = SNSBuilder.buildSNSWriter(config)
  )

  try {
    info(s"Starting worker.")

    val result = workerService.run()

    Await.result(result, Duration.Inf)
  } catch {
    case e: Throwable =>
      error("Fatal error:", e)
  } finally {
    info(s"Terminating worker.")
  }
}
