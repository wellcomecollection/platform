package uk.ac.wellcome.platform.snapshot_generator

import akka.actor.ActorSystem
import com.typesafe.config.Config
import uk.ac.wellcome.config.core.WellcomeTypesafeApp
import uk.ac.wellcome.config.core.builders.AkkaBuilder
import uk.ac.wellcome.config.elasticsearch.builders.ElasticBuilder
import uk.ac.wellcome.config.messaging.builders.{SNSBuilder, SQSBuilder}
import uk.ac.wellcome.display.modules.DisplayJacksonModule
import uk.ac.wellcome.messaging.sns.NotificationMessage
import uk.ac.wellcome.platform.snapshot_generator.config.builders.AkkaS3Builder
import uk.ac.wellcome.platform.snapshot_generator.services.{SnapshotGeneratorWorkerService, SnapshotService}

import scala.concurrent.ExecutionContext

object Main extends WellcomeTypesafeApp {
  runWithConfig { config: Config =>
    implicit val actorSystem: ActorSystem = AkkaBuilder.buildActorSystem()
    implicit val executionContext: ExecutionContext =
      AkkaBuilder.buildExecutionContext()

    new SnapshotGeneratorWorkerService(
      snapshotService = new SnapshotService(
        akkaS3Client = AkkaS3Builder.buildAkkaS3Client(config),
        elasticClient = ElasticBuilder.buildHttpClient(config),
        elasticConfig = ElasticBuilder.buildElasticConfig(config),
        objectMapper = DisplayJacksonModule.provideScalaObjectMapper(null)
      ),
      sqsStream = SQSBuilder.buildSQSStream[NotificationMessage](config),
      snsWriter = SNSBuilder.buildSNSWriter(config)
    )
  }
}
