package uk.ac.wellcome.platform.transformer.miro

import akka.actor.ActorSystem
import com.typesafe.config.{Config, ConfigFactory}
import grizzled.slf4j.Logging
import uk.ac.wellcome.config.core.builders.AkkaBuilder
import uk.ac.wellcome.config.messaging.builders.{MessagingBuilder, SQSBuilder}
import uk.ac.wellcome.config.storage.builders.S3Builder
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.messaging.sns.NotificationMessage
import uk.ac.wellcome.models.work.internal.TransformedBaseWork
import uk.ac.wellcome.platform.transformer.miro.services.{MiroTransformerWorkerService, MiroVHSRecordReceiver}
import uk.ac.wellcome.platform.transformer.miro.source.MiroRecord

import scala.concurrent.{Await, ExecutionContext}
import scala.concurrent.duration.Duration

object Main extends App with Logging {
  val config: Config = ConfigFactory.load()

  implicit val actorSystem: ActorSystem =
    AkkaBuilder.buildActorSystem()
  implicit val executionContext: ExecutionContext =
    AkkaBuilder.buildExecutionContext()

  val vhsRecordReceiver = new MiroVHSRecordReceiver(
    messageWriter =
      MessagingBuilder.buildMessageWriter[TransformedBaseWork](config),
    objectStore = S3Builder.buildObjectStore[MiroRecord](config)
  )

  val workerService = new MiroTransformerWorkerService(
    vhsRecordReceiver = vhsRecordReceiver,
    miroTransformer = new MiroTransformableTransformer,
    sqsStream = SQSBuilder.buildSQSStream[NotificationMessage](config)
  )

  try {
    info("Starting worker.")

    val result = workerService.run()

    Await.result(result, Duration.Inf)
  } catch {
    case e: Throwable =>
      error("Fatal error:", e)
  } finally {
    info("Terminating worker.")
  }
}
