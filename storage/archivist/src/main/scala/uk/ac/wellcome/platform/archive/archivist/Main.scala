package uk.ac.wellcome.platform.archive.archivist

import akka.actor.ActorSystem
import com.typesafe.config.Config
import uk.ac.wellcome.config.core.WellcomeTypesafeApp
import uk.ac.wellcome.config.core.builders.AkkaBuilder
import uk.ac.wellcome.config.messaging.builders.SNSBuilder
import uk.ac.wellcome.config.storage.builders.S3Builder
import uk.ac.wellcome.messaging.sns.NotificationMessage
import uk.ac.wellcome.platform.archive.archivist.builders.TransferManagerBuilder
import uk.ac.wellcome.platform.archive.archivist.config.BagUploaderConfigBuilder
import uk.ac.wellcome.platform.archive.common.config.builders.MessagingBuilder

object Main extends WellcomeTypesafeApp {
  runWithConfig { config: Config =>
    implicit val actorSystem: ActorSystem = AkkaBuilder.buildActorSystem()
    implicit val s3Client = S3Builder.buildS3Client(config)
    implicit val transferManager =
      TransferManagerBuilder.buildTransferManager(
        S3Builder.buildS3Client(config))
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
