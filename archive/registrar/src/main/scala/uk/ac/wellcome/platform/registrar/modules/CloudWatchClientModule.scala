package uk.ac.wellcome.platform.registrar.modules

import com.amazonaws.services.cloudwatch.AmazonCloudWatch
import com.google.inject.{AbstractModule, Provides, Singleton}
import uk.ac.wellcome.monitoring.CloudWatchClientFactory
import uk.ac.wellcome.platform.registrar.models.CloudwatchClientConfig

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
