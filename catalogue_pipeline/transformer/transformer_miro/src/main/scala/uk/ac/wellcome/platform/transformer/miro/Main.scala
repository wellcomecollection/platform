package uk.ac.wellcome.platform.transformer.miro

import akka.actor.ActorSystem
import com.typesafe.config.{Config, ConfigFactory}
import uk.ac.wellcome.WellcomeApp
import uk.ac.wellcome.config.core.builders.AkkaBuilder
import uk.ac.wellcome.config.messaging.builders.{MessagingBuilder, SQSBuilder}
import uk.ac.wellcome.config.storage.builders.S3Builder
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.messaging.sns.NotificationMessage
import uk.ac.wellcome.models.work.internal.TransformedBaseWork
import uk.ac.wellcome.platform.transformer.miro.models.MiroTransformable
import uk.ac.wellcome.platform.transformer.miro.services.MiroTransformerWorkerService
import uk.ac.wellcome.platform.transformer.receive.HybridRecordReceiver

import scala.concurrent.ExecutionContext

object Main extends WellcomeApp {
  val config: Config = ConfigFactory.load()

  implicit val actorSystem: ActorSystem =
    AkkaBuilder.buildActorSystem()
  implicit val executionContext: ExecutionContext =
    AkkaBuilder.buildExecutionContext()

  val messageReceiver = new HybridRecordReceiver[MiroTransformable](
    messageWriter =
      MessagingBuilder.buildMessageWriter[TransformedBaseWork](config),
    objectStore = S3Builder.buildObjectStore[MiroTransformable](config)
  )

  val workerService = new MiroTransformerWorkerService(
    messageReceiver = messageReceiver,
    miroTransformer = new MiroTransformableTransformer,
    sqsStream = SQSBuilder.buildSQSStream[NotificationMessage](config)
  )

  run(workerService)
}
