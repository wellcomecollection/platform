package uk.ac.wellcome.platform.archive.bagreplicator

import akka.actor.ActorSystem
import com.typesafe.config.Config
import uk.ac.wellcome.messaging.sns.NotificationMessage
import uk.ac.wellcome.messaging.typesafe.SNSBuilder
import uk.ac.wellcome.platform.archive.bagreplicator.config.BagReplicatorConfig
import uk.ac.wellcome.platform.archive.common.config.builders.MessagingBuilder
import uk.ac.wellcome.storage.typesafe.S3Builder
import uk.ac.wellcome.typesafe.WellcomeTypesafeApp
import uk.ac.wellcome.typesafe.config.builders.AkkaBuilder

object Main extends WellcomeTypesafeApp {
  runWithConfig { config: Config =>
    implicit val actorSystem: ActorSystem = AkkaBuilder.buildActorSystem()

    new BagReplicator(
      s3Client = S3Builder.buildS3Client(config),
      snsClient = SNSBuilder.buildSNSClient(config),
      messageStream =
        MessagingBuilder.buildMessageStream[NotificationMessage, Unit](config),
      bagReplicatorConfig = BagReplicatorConfig.buildBagReplicatorConfig(config),
      progressSnsConfig =
        SNSBuilder.buildSNSConfig(config, namespace = "progress"),
      outgoingSnsConfig =
        SNSBuilder.buildSNSConfig(config, namespace = "outgoing")
    )
  }
}
