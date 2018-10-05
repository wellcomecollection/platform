package uk.ac.wellcome.platform.archive.common.modules

import com.amazonaws.services.cloudwatch.AmazonCloudWatch
import com.google.inject.{AbstractModule, Provides, Singleton}
import com.typesafe.config.Config
import uk.ac.wellcome.monitoring.CloudWatchClientFactory
import uk.ac.wellcome.platform.archive.common.models.EnrichConfig

object CloudWatchClientModule extends AbstractModule {
  import EnrichConfig._

  @Provides
  @Singleton
  def providesCloudWatchClientConfig(config: Config) = {
    val endpoint = config
      .get[String]("aws.cloudwatch.endpoint")

    val region = config
      .getOrElse[String]("aws.cloudwatch.region")("eu-west-1")

    CloudwatchConfig(endpoint, region)
  }

  @Provides
  @Singleton
  def providesAmazonCloudWatch(
                                serviceClientConfig: CloudwatchConfig): AmazonCloudWatch =
    CloudWatchClientFactory.create(
      region = serviceClientConfig.region,
      endpoint = serviceClientConfig
        .endpoint.getOrElse("")
    )
}

case class CloudwatchConfig(
                             endpoint: Option[String],
                             region: String
                           )