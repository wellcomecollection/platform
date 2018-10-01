package uk.ac.wellcome.platform.archive.call_backerei.modules

import com.google.inject.{AbstractModule, Provides}
import uk.ac.wellcome.platform.archive.call_backerei.models.CallBäckereiConfig

object ConfigModule extends AbstractModule {
  @Provides
  def providesCloudwatchClientConfig(appConfig: CallBäckereiConfig) =
    appConfig.cloudwatchClientConfig

  @Provides
  def providesSQSConfig(appConfig: CallBäckereiConfig) =
    appConfig.sqsConfig

  @Provides
  def providesSQSClientConfig(appConfig: CallBäckereiConfig) =
    appConfig.sqsClientConfig

  @Provides
  def providesMetricsConfig(appConfig: CallBäckereiConfig) =
    appConfig.metricsConfig

  @Provides
  def providesSNSConfig(appConfig: CallBäckereiConfig) =
    appConfig.snsConfig

  @Provides
  def providesSNSClientConfig(appConfig: CallBäckereiConfig) =
    appConfig.snsClientConfig
}
