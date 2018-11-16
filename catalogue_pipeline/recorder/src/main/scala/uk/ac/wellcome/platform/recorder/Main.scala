package uk.ac.wellcome.platform.recorder

import akka.actor.ActorSystem
import com.typesafe.config.Config
import uk.ac.wellcome.WellcomeApp
import uk.ac.wellcome.config.core.builders.AkkaBuilder
import uk.ac.wellcome.config.messaging.builders.{MessagingBuilder, SNSBuilder}
import uk.ac.wellcome.config.storage.builders.VHSBuilder
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.models.work.internal.TransformedBaseWork
import uk.ac.wellcome.platform.recorder.services.RecorderWorkerService
import uk.ac.wellcome.storage.vhs.EmptyMetadata

import scala.concurrent.ExecutionContext

object Main extends WellcomeApp {
  def buildWorkerService(config: Config): RecorderWorkerService = {
    implicit val actorSystem: ActorSystem =
      AkkaBuilder.buildActorSystem()
    implicit val executionContext: ExecutionContext =
      AkkaBuilder.buildExecutionContext()

    val workerService = new RecorderWorkerService(
      versionedHybridStore =
        VHSBuilder.buildVHS[TransformedBaseWork, EmptyMetadata](config),
      messageStream =
        MessagingBuilder.buildMessageStream[TransformedBaseWork](config),
      snsWriter = SNSBuilder.buildSNSWriter(config)
    )
  }

  run()
}
