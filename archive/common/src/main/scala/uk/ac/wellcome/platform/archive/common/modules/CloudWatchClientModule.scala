package uk.ac.wellcome.platform.archive.common.modules

import com.amazonaws.services.cloudwatch.AmazonCloudWatch
import com.google.inject.{AbstractModule, Provides, Singleton}
import uk.ac.wellcome.monitoring.CloudWatchClientFactory

object CloudWatchClientModule extends AbstractModule {
  @Provides
  @Singleton
  def providesAmazonCloudWatch(
    serviceClientConfig: CloudwatchClientConfig): AmazonCloudWatch =
    CloudWatchClientFactory.create(
      region = serviceClientConfig.region,
      endpoint = serviceClientConfig.endpoint.getOrElse("")
    )
}

case class CloudwatchClientConfig(
                                   endpoint: Option[String],
                                   region: String
                                 )