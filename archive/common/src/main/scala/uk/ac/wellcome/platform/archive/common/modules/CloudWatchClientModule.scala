package uk.ac.wellcome.platform.archive.common.modules

import com.amazonaws.services.cloudwatch.AmazonCloudWatch
import com.google.inject.{AbstractModule, Provides, Singleton}
import uk.ac.wellcome.monitoring.CloudWatchClientFactory
import uk.ac.wellcome.platform.archive.common.modules.config.CloudwatchConfig

object CloudWatchClientModule extends AbstractModule {
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
