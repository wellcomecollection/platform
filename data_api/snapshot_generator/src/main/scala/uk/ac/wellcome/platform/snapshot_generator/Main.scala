package uk.ac.wellcome.platform.snapshot_generator

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.typesafe.config.Config
import uk.ac.wellcome.config.elasticsearch.builders.ElasticBuilder
import uk.ac.wellcome.messaging.sns.NotificationMessage
import uk.ac.wellcome.messaging.typesafe.{SNSBuilder, SQSBuilder}
import uk.ac.wellcome.platform.snapshot_generator.config.builders.AkkaS3Builder
import uk.ac.wellcome.platform.snapshot_generator.services.{
  SnapshotGeneratorWorkerService,
  SnapshotService
}
import uk.ac.wellcome.typesafe.WellcomeTypesafeApp
import uk.ac.wellcome.typesafe.config.builders.AkkaBuilder

import scala.concurrent.ExecutionContext

object Main extends WellcomeTypesafeApp {
  runWithConfig { config: Config =>
    implicit val actorSystem: ActorSystem = AkkaBuilder.buildActorSystem()
    implicit val executionContext: ExecutionContext =
      AkkaBuilder.buildExecutionContext()
    implicit val materializer: ActorMaterializer =
      AkkaBuilder.buildActorMaterializer()

    val snapshotService = new SnapshotService(
      akkaS3Client = AkkaS3Builder.buildAkkaS3Client(config),
      elasticClient = ElasticBuilder.buildElasticClient(config),
      elasticConfig = ElasticBuilder.buildElasticConfig(config)
    )

    new SnapshotGeneratorWorkerService(
      snapshotService = snapshotService,
      sqsStream = SQSBuilder.buildSQSStream[NotificationMessage](config),
      snsWriter = SNSBuilder.buildSNSWriter(config)
    )
  }
}
