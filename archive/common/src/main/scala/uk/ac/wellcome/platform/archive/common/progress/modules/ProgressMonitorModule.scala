package uk.ac.wellcome.platform.archive.common.progress.modules

import akka.actor.ActorSystem
import com.google.inject.{AbstractModule, Provides}
import uk.ac.wellcome.platform.archive.common.config.models.ProgressMonitorConfig
import uk.ac.wellcome.platform.archive.common.progress.monitor.ProgressMonitor
import uk.ac.wellcome.storage.dynamo.DynamoClientFactory

object ProgressMonitorModule extends AbstractModule {

  @Provides
  def providesArchiveProgressMonitor(
    actorSystem: ActorSystem,
    progressMonitorConfig: ProgressMonitorConfig) = {

    val dynamoClientConfig = progressMonitorConfig.dynamoClientConfig

    val dynamoClient = DynamoClientFactory.create(
      region = dynamoClientConfig.region,
      endpoint = dynamoClientConfig.endpoint.getOrElse(""),
      accessKey = dynamoClientConfig.accessKey.getOrElse(""),
      secretKey = dynamoClientConfig.secretKey.getOrElse("")
    )

    new ProgressMonitor(
      dynamoClient,
      progressMonitorConfig.dynamoConfig
    )
  }
}
