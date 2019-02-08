package uk.ac.wellcome.platform.transformer.miro

import akka.actor.ActorSystem
import com.typesafe.config.Config
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.messaging.sns.NotificationMessage
import uk.ac.wellcome.messaging.typesafe.{MessagingBuilder, SQSBuilder}
import uk.ac.wellcome.models.work.internal.TransformedBaseWork
import uk.ac.wellcome.platform.transformer.miro.services.{
  MiroTransformerWorkerService,
  MiroVHSRecordReceiver
}
import uk.ac.wellcome.platform.transformer.miro.source.MiroRecord
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

    val vhsRecordReceiver = new MiroVHSRecordReceiver(
      messageWriter =
        MessagingBuilder.buildMessageWriter[TransformedBaseWork](config),
      objectStore = S3Builder.buildObjectStore[MiroRecord](config)
    )

    new MiroTransformerWorkerService(
      vhsRecordReceiver = vhsRecordReceiver,
      miroTransformer = new MiroRecordTransformer,
      sqsStream = SQSBuilder.buildSQSStream[NotificationMessage](config)
    )
  }
}
