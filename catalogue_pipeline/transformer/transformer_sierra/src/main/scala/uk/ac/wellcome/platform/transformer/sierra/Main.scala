package uk.ac.wellcome.platform.transformer.sierra

import akka.actor.ActorSystem
import com.typesafe.config.Config
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.messaging.sns.NotificationMessage
import uk.ac.wellcome.messaging.typesafe.{MessagingBuilder, SQSBuilder}
import uk.ac.wellcome.models.transformable.SierraTransformable
import uk.ac.wellcome.models.transformable.SierraTransformable._
import uk.ac.wellcome.models.work.internal.TransformedBaseWork
import uk.ac.wellcome.platform.transformer.sierra.services.{
  HybridRecordReceiver,
  SierraTransformerWorkerService
}
import uk.ac.wellcome.storage.typesafe.S3Builder
import uk.ac.wellcome.typesafe.WellcomeTypesafeApp
import uk.ac.wellcome.typesafe.config.builders.AkkaBuilder

import scala.concurrent.ExecutionContext

object Main extends WellcomeTypesafeApp {
  runWithConfig { config: Config =>
    implicit val actorSystem: ActorSystem =
      AkkaBuilder.buildActorSystem()
    implicit val executionContext: ExecutionContext =
      AkkaBuilder.buildExecutionContext()

    val messageReceiver = new HybridRecordReceiver[SierraTransformable](
      messageWriter =
        MessagingBuilder.buildMessageWriter[TransformedBaseWork](config),
      objectStore = S3Builder.buildObjectStore[SierraTransformable](config)
    )

    new SierraTransformerWorkerService(
      messageReceiver = messageReceiver,
      sierraTransformer = new SierraTransformableTransformer(),
      sqsStream = SQSBuilder.buildSQSStream[NotificationMessage](config)
    )
  }
}
