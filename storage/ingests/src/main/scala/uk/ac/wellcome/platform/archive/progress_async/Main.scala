package uk.ac.wellcome.platform.archive.progress_async

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.typesafe.config.Config
import uk.ac.wellcome.messaging.sns.NotificationMessage
import uk.ac.wellcome.messaging.typesafe.SNSBuilder
import uk.ac.wellcome.platform.archive.common.config.builders._
import uk.ac.wellcome.platform.archive.common.progress.monitor.ProgressTracker
import uk.ac.wellcome.storage.typesafe.DynamoBuilder
import uk.ac.wellcome.typesafe.WellcomeTypesafeApp
import uk.ac.wellcome.typesafe.config.builders.AkkaBuilder

import scala.concurrent.ExecutionContext

object Main extends WellcomeTypesafeApp {
  runWithConfig { config: Config =>
    implicit val actorSystem: ActorSystem = AkkaBuilder.buildActorSystem()
    implicit val materializer: ActorMaterializer =
      AkkaBuilder.buildActorMaterializer()
    implicit val executionContext: ExecutionContext =
      AkkaBuilder.buildExecutionContext()

    new ProgressAsync(
      messageStream =
        MessagingBuilder.buildMessageStream[NotificationMessage, Unit](config),
      progressTracker = new ProgressTracker(
        dynamoDbClient = DynamoBuilder.buildDynamoClient(config),
        dynamoConfig = DynamoBuilder.buildDynamoConfig(config)
      ),
      snsClient = SNSBuilder.buildSNSClient(config),
      snsConfig = SNSBuilder.buildSNSConfig(config)
    )
  }
}
