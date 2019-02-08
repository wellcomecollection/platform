package uk.ac.wellcome.platform.archive.registrar.async

import akka.actor.ActorSystem
import com.typesafe.config.Config
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.messaging.sns.NotificationMessage
import uk.ac.wellcome.messaging.typesafe.SNSBuilder
import uk.ac.wellcome.platform.archive.common.config.builders._
import uk.ac.wellcome.platform.archive.registrar.common.models.StorageManifest
import uk.ac.wellcome.storage.typesafe.{S3Builder, VHSBuilder}
import uk.ac.wellcome.storage.vhs.EmptyMetadata
import uk.ac.wellcome.typesafe.WellcomeTypesafeApp
import uk.ac.wellcome.typesafe.config.builders.AkkaBuilder

object Main extends WellcomeTypesafeApp {
  runWithConfig { config: Config =>
    implicit val actorSystem: ActorSystem = AkkaBuilder.buildActorSystem()

    val messageStream =
      MessagingBuilder.buildMessageStream[NotificationMessage, Unit](config)

    val dataStore = VHSBuilder.buildVHS[StorageManifest, EmptyMetadata](config)

    new Registrar(
      snsClient = SNSBuilder.buildSNSClient(config),
      progressSnsConfig = SNSBuilder.buildSNSConfig(config),
      s3Client = S3Builder.buildS3Client(config),
      messageStream = messageStream,
      dataStore = dataStore
    )
  }
}
