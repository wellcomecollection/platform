package uk.ac.wellcome.platform.archive.common.progress.modules

import akka.actor.ActorSystem
import com.google.inject.{AbstractModule, Provides}
import com.typesafe.config.Config
import javax.inject.Singleton
import uk.ac.wellcome.platform.archive.common.models.EnrichConfig
import uk.ac.wellcome.platform.archive.common.modules.{AkkaModule, DynamoClientConfig}
import uk.ac.wellcome.platform.archive.common.progress.monitor.ProgressMonitor
import uk.ac.wellcome.storage.dynamo.{DynamoClientFactory, DynamoConfig}

object ProgressMonitorModule extends AbstractModule {
  install(AkkaModule)

  import EnrichConfig._

  @Provides
  @Singleton
  def providesProgressMonitorConfig(config: Config) = {
    val table = config
      .required[String]("progress.table.name")

    val key = config
      .get[String]("progress.table.key")

    val secret = config
      .get[String]("progress.table.secret")

    val endpoint = config
      .get[String]("progress.table.endpoint")

    val region = config
      .getOrElse[String]("progress.table.endpoint")("eu-west-1")

    ProgressMonitorConfig(
      DynamoConfig(table, None),
      DynamoClientConfig(key, secret, endpoint, region)
    )
  }

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
case class ProgressMonitorConfig(dynamoConfig: DynamoConfig,
                                 dynamoClientConfig: DynamoClientConfig)
