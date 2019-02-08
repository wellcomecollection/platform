package uk.ac.wellcome.platform.archive.archivist

import akka.actor.ActorSystem
import com.typesafe.config.Config
import uk.ac.wellcome.messaging.sns.NotificationMessage
import uk.ac.wellcome.messaging.typesafe.SNSBuilder
import uk.ac.wellcome.platform.archive.archivist.builders.TransferManagerBuilder
import uk.ac.wellcome.platform.archive.archivist.config.BagUploaderConfigBuilder
import uk.ac.wellcome.platform.archive.common.config.builders.MessagingBuilder
import uk.ac.wellcome.storage.typesafe.S3Builder
import uk.ac.wellcome.typesafe.WellcomeTypesafeApp
import uk.ac.wellcome.typesafe.config.builders.AkkaBuilder

object Main extends WellcomeTypesafeApp {
  runWithConfig { config: Config =>
    implicit val actorSystem: ActorSystem = AkkaBuilder.buildActorSystem()
    implicit val s3Client = S3Builder.buildS3Client(config)
    implicit val transferManager =
      TransferManagerBuilder.buildTransferManager(config)
    implicit val snsClient = SNSBuilder.buildSNSClient(config)

    new Archivist(
      messageStream =
        MessagingBuilder.buildMessageStream[NotificationMessage, Unit](config),
      bagUploaderConfig =
        BagUploaderConfigBuilder.buildBagUploaderConfig(config),
      snsNextConfig = SNSBuilder.buildSNSConfig(config, namespace = "next"),
      snsProgressConfig =
        SNSBuilder.buildSNSConfig(config, namespace = "progress")
    )
  }
}
