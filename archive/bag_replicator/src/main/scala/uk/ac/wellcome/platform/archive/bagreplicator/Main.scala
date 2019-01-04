package uk.ac.wellcome.platform.archive.bagreplicator

import akka.actor.ActorSystem
import com.typesafe.config.Config
import uk.ac.wellcome.config.core.WellcomeTypesafeApp
import uk.ac.wellcome.config.core.builders.AkkaBuilder
import uk.ac.wellcome.config.messaging.builders.SNSBuilder
import uk.ac.wellcome.config.storage.builders.S3Builder
import uk.ac.wellcome.platform.archive.bagreplicator.config.BagReplicatorConfig
import uk.ac.wellcome.platform.archive.common.config.builders.MessagingBuilder
import uk.ac.wellcome.platform.archive.common.models.NotificationMessage

object Main extends WellcomeTypesafeApp {
  runWithConfig { config: Config =>
    implicit val actorSystem: ActorSystem = AkkaBuilder.buildActorSystem()

    new BagReplicator(
      s3Client = S3Builder.buildS3Client(config),
      snsClient = SNSBuilder.buildSNSClient(config),
      messageStream =
        MessagingBuilder.buildMessageStream[NotificationMessage, Unit](config),
      bagReplicatorConfig = BagReplicatorConfig.buildBagUploaderConfig(config),
      snsProgressConfig =
        SNSBuilder.buildSNSConfig(config, namespace = "progress")
    )
  }
}
