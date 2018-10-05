package uk.ac.wellcome.platform.archive.common.modules.config

import com.google.inject.{AbstractModule, Provides}
import com.typesafe.config.Config
import javax.inject.Singleton
import uk.ac.wellcome.platform.archive.common.modules.DynamoClientConfig
import uk.ac.wellcome.platform.archive.common.progress.modules.ProgressMonitorConfig
import uk.ac.wellcome.storage.dynamo.DynamoConfig

object ProgressMonitorConfigModule extends AbstractModule {

  import ConfigHelper._

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
}
