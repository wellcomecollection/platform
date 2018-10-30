package uk.ac.wellcome.platform.archive.common.progress.modules

import akka.actor.ActorSystem
import com.google.inject.{AbstractModule, Provides}
import uk.ac.wellcome.platform.archive.common.progress.monitor.ProgressTracker
import uk.ac.wellcome.storage.dynamo.DynamoClientFactory

import scala.concurrent.ExecutionContext

object ProgressTrackerModule extends AbstractModule {

  @Provides
  def providesArchiveProgressTracker(
    actorSystem: ActorSystem,
    progressTrackerConfig: ProgressTrackerConfig)(implicit ec: ExecutionContext) = {

    val dynamoClientConfig = progressTrackerConfig.dynamoClientConfig

    val dynamoClient = DynamoClientFactory.create(
      region = dynamoClientConfig.region,
      endpoint = dynamoClientConfig.endpoint.getOrElse(""),
      accessKey = dynamoClientConfig.accessKey.getOrElse(""),
      secretKey = dynamoClientConfig.secretKey.getOrElse("")
    )

    new ProgressTracker(
      dynamoClient,
      progressTrackerConfig.dynamoConfig
    )
  }
}
