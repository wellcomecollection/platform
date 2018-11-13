package uk.ac.wellcome.platform.transformer.sierra

import com.typesafe.config.{Config, ConfigFactory}
import grizzled.slf4j.Logging
import uk.ac.wellcome.config.core.builders.AkkaBuilder
import uk.ac.wellcome.config.messaging.builders.{MessagingBuilder, SQSBuilder}
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.messaging.sns.NotificationMessage
import uk.ac.wellcome.models.transformable.SierraTransformable
import uk.ac.wellcome.models.transformable.SierraTransformable._
import uk.ac.wellcome.models.work.internal.TransformedBaseWork
import uk.ac.wellcome.platform.transformer.receive.HybridRecordReceiver
import uk.ac.wellcome.platform.transformer.sierra.services.SierraTransformerWorkerService
import uk.ac.wellcome.storage.ObjectStore

import scala.concurrent.{Await, ExecutionContext}
import scala.concurrent.duration.Duration

class Main extends App with Logging {
  val config: Config = ConfigFactory.load()

  implicit val executionContext: ExecutionContext =
    AkkaBuilder.buildExecutionContext()

  val messageReceiver = new HybridRecordReceiver[SierraTransformable](
    messageWriter = MessagingBuilder.buildMessageWriter[TransformedBaseWork](config),
    objectStore = ObjectStore[SierraTransformable]
  )

  val workerService = new SierraTransformerWorkerService(
    messageReceiver = messageReceiver,
    sierraTransformer = new SierraTransformableTransformer(),
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
