package uk.ac.wellcome.platform.archive.common.modules.config

import com.google.inject.{AbstractModule, Provides, Singleton}
import com.typesafe.config.Config

object CloudwatchConfigModule extends AbstractModule {

  import ConfigHelper._

  @Provides
  @Singleton
  def providesCloudwatchClientConfig(config: Config) = {
    val endpoint = config
      .get[String]("aws.cloudwatch.endpoint")

    val region = config
      .getOrElse[String]("aws.cloudwatch.region")("eu-west-1")

    CloudwatchConfig(endpoint, region)
  }
}


case class CloudwatchConfig(
                             endpoint: Option[String],
                             region: String
                           )
