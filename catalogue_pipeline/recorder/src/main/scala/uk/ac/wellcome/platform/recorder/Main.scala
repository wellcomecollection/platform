package uk.ac.wellcome.platform.recorder

import akka.actor.ActorSystem
import com.typesafe.config.Config
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.messaging.typesafe.{MessagingBuilder, SNSBuilder}
import uk.ac.wellcome.models.work.internal.TransformedBaseWork
import uk.ac.wellcome.platform.recorder.services.RecorderWorkerService
import uk.ac.wellcome.storage.typesafe.VHSBuilder
import uk.ac.wellcome.storage.vhs.EmptyMetadata
import uk.ac.wellcome.typesafe.WellcomeTypesafeApp
import uk.ac.wellcome.typesafe.config.builders.AkkaBuilder

import scala.concurrent.ExecutionContext

object Main extends WellcomeTypesafeApp {
  runWithConfig { config: Config =>
    implicit val actorSystem: ActorSystem =
      AkkaBuilder.buildActorSystem()
    implicit val executionContext: ExecutionContext =
      AkkaBuilder.buildExecutionContext()

    new RecorderWorkerService(
      versionedHybridStore =
        VHSBuilder.buildVHS[TransformedBaseWork, EmptyMetadata](config),
      messageStream =
        MessagingBuilder.buildMessageStream[TransformedBaseWork](config),
      snsWriter = SNSBuilder.buildSNSWriter(config)
    )
  }
}
