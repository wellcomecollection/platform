package uk.ac.wellcome.platform.archive.progress_async

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.typesafe.config.Config
import uk.ac.wellcome.WellcomeApp
import uk.ac.wellcome.config.core.builders.AkkaBuilder
import uk.ac.wellcome.config.messaging.builders.SNSBuilder
import uk.ac.wellcome.config.storage.builders.DynamoBuilder
import uk.ac.wellcome.platform.archive.common.config.builders._
import uk.ac.wellcome.platform.archive.common.models.NotificationMessage
import uk.ac.wellcome.platform.archive.common.progress.monitor.ProgressTracker

import scala.concurrent.ExecutionContext

object Main extends WellcomeApp {
  def buildWorkerService(config: Config): ProgressAsync = {
    implicit val actorSystem: ActorSystem = AkkaBuilder.buildActorSystem()
    implicit val materializer: ActorMaterializer =
      AkkaBuilder.buildActorMaterializer()
    implicit val executionContext: ExecutionContext =
      AkkaBuilder.buildExecutionContext()

    new ProgressAsync(
      messageStream =
        MessagingBuilder.buildMessageStream[NotificationMessage, Unit](config),
      progressTracker = new ProgressTracker(
        dynamoClient = DynamoBuilder.buildDynamoClient(config),
        dynamoConfig = DynamoBuilder.buildDynamoConfig(config)
      ),
      snsClient = SNSBuilder.buildSNSClient(config),
      snsConfig = SNSBuilder.buildSNSConfig(config)
    )
  }

  run()
}
