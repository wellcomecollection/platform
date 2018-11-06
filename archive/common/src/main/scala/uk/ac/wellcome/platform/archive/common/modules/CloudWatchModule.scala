package uk.ac.wellcome.platform.archive.common.modules

import com.amazonaws.services.cloudwatch.AmazonCloudWatch
import com.google.inject.{AbstractModule, Provides, Singleton}
import com.typesafe.config.Config
import uk.ac.wellcome.monitoring.CloudWatchClientFactory
import uk.ac.wellcome.platform.archive.common.models.EnrichConfig._

object CloudWatchModule extends AbstractModule {

  @Singleton
  @Provides
  def providesSNSClientConfig(config: Config): CloudwatchClientConfig = {
    val endpoint = config.get[String]("aws.cloudwatch.endpoint")
    val region = config.getOrElse[String]("aws.cloudwatch.region")("eu-west-1")

    CloudwatchClientConfig(
      endpoint = endpoint,
      region = region
    )
  }

  @Provides
  @Singleton
  def providesAmazonCloudWatch(clientConfig: CloudwatchClientConfig): AmazonCloudWatch =
    CloudWatchClientFactory.create(
      region = clientConfig.region,
      endpoint = clientConfig.endpoint.getOrElse("")
    )
}
