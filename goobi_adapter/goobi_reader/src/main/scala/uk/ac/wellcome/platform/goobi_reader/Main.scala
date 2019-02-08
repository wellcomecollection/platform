package uk.ac.wellcome.platform.goobi_reader

import java.io.InputStream

import akka.actor.ActorSystem
import com.typesafe.config.Config
import uk.ac.wellcome.messaging.sns.NotificationMessage
import uk.ac.wellcome.messaging.typesafe.SQSBuilder
import uk.ac.wellcome.platform.goobi_reader.models.GoobiRecordMetadata
import uk.ac.wellcome.platform.goobi_reader.services.GoobiReaderWorkerService
import uk.ac.wellcome.storage.typesafe.{S3Builder, VHSBuilder}
import uk.ac.wellcome.typesafe.WellcomeTypesafeApp
import uk.ac.wellcome.typesafe.config.builders.AkkaBuilder

import scala.concurrent.ExecutionContext

object Main extends WellcomeTypesafeApp {
  runWithConfig { config: Config =>
    implicit val actorSystem: ActorSystem = AkkaBuilder.buildActorSystem()
    implicit val executionContext: ExecutionContext =
      AkkaBuilder.buildExecutionContext()

    new GoobiReaderWorkerService(
      s3Client = S3Builder.buildS3Client(config),
      sqsStream = SQSBuilder.buildSQSStream[NotificationMessage](config),
      versionedHybridStore =
        VHSBuilder.buildVHS[InputStream, GoobiRecordMetadata](config)
    )
  }
}
