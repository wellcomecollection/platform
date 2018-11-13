package uk.ac.wellcome.platform.transformer.miro

import com.typesafe.config.{Config, ConfigFactory}
import grizzled.slf4j.Logging
import uk.ac.wellcome.config.messaging.builders.{MessagingBuilder, SQSBuilder}
import uk.ac.wellcome.messaging.sns.NotificationMessage
import uk.ac.wellcome.models.work.internal.TransformedBaseWork
import uk.ac.wellcome.platform.transformer.miro.models.MiroTransformable
import uk.ac.wellcome.platform.transformer.miro.services.MiroTransformerWorkerService
import uk.ac.wellcome.platform.transformer.receive.HybridRecordReceiver
import uk.ac.wellcome.storage.ObjectStore

import scala.concurrent.Await
import scala.concurrent.duration.Duration

object Main extends App with Logging {
  val config: Config = ConfigFactory.load()

  val messageReceiver = new HybridRecordReceiver[MiroTransformable](
    messageWriter = MessagingBuilder.buildMessageWriter[TransformedBaseWork](config),
    objectStore = ObjectStore[MiroTransformable]
  )

  val workerService = new MiroTransformerWorkerService(
    messageReceiver = messageReceiver,
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
