package uk.ac.wellcome.platform.merger

import akka.actor.ActorSystem
import com.typesafe.config.{Config, ConfigFactory}
import grizzled.slf4j.Logging
import uk.ac.wellcome.config.core.builders.AkkaBuilder
import uk.ac.wellcome.config.messaging.builders.{MessagingBuilder, SQSBuilder}
import uk.ac.wellcome.config.storage.builders.VHSBuilder
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.messaging.sns.NotificationMessage
import uk.ac.wellcome.models.work.internal.{BaseWork, TransformedBaseWork}
import uk.ac.wellcome.platform.merger.services.{
  Merger,
  MergerManager,
  MergerWorkerService,
  RecorderPlaybackService
}
import uk.ac.wellcome.storage.vhs.EmptyMetadata

import scala.concurrent.{Await, ExecutionContext}
import scala.concurrent.duration.Duration

object Main extends App with Logging {
  val config: Config = ConfigFactory.load()

  implicit val actorSystem: ActorSystem =
    AkkaBuilder.buildActorSystem()
  implicit val executionContext: ExecutionContext =
    AkkaBuilder.buildExecutionContext()

  val playbackService = new RecorderPlaybackService(
    versionedHybridStore =
      VHSBuilder.buildVHS[TransformedBaseWork, EmptyMetadata](config)
  )

  val mergerManager = new MergerManager(
    mergerRules = new Merger()
  )

  val workerService = new MergerWorkerService(
    sqsStream = SQSBuilder.buildSQSStream[NotificationMessage](config),
    playbackService = playbackService,
    mergerManager = mergerManager,
    messageWriter = MessagingBuilder.buildMessageWriter[BaseWork](config)
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
