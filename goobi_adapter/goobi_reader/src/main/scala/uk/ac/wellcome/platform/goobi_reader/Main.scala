package uk.ac.wellcome.platform.goobi_reader

import java.io.InputStream

import akka.actor.ActorSystem
import com.typesafe.config.{Config, ConfigFactory}
import uk.ac.wellcome.WellcomeApp
import uk.ac.wellcome.config.core.builders.AkkaBuilder
import uk.ac.wellcome.config.messaging.builders.SQSBuilder
import uk.ac.wellcome.config.storage.builders.{S3Builder, VHSBuilder}
import uk.ac.wellcome.messaging.sns.NotificationMessage
import uk.ac.wellcome.platform.goobi_reader.models.GoobiRecordMetadata
import uk.ac.wellcome.platform.goobi_reader.services.GoobiReaderWorkerService

object Main extends WellcomeApp {
  val config: Config = ConfigFactory.load()

  implicit val actorSystem: ActorSystem = AkkaBuilder.buildActorSystem()

  val workerService = new GoobiReaderWorkerService(
    s3Client = S3Builder.buildS3Client(config),
    sqsStream = SQSBuilder.buildSQSStream[NotificationMessage](config),
    versionedHybridStore =
      VHSBuilder.buildVHS[InputStream, GoobiRecordMetadata](config)
  )

  run(workerService)
}
