package uk.ac.wellcome.platform.archive.call_backerei.modules

import com.google.inject.{AbstractModule, Provides}
import uk.ac.wellcome.platform.archive.call_backerei.models.CallBackereiConfig

object ConfigModule extends AbstractModule {
  @Provides
  def providesCloudwatchClientConfig(appConfig: CallBackereiConfig) =
    appConfig.cloudwatchClientConfig

  @Provides
  def providesSQSConfig(appConfig: CallBackereiConfig) =
    appConfig.sqsConfig

  @Provides
  def providesSQSClientConfig(appConfig: CallBackereiConfig) =
    appConfig.sqsClientConfig

  @Provides
  def providesMetricsConfig(appConfig: CallBackereiConfig) =
    appConfig.metricsConfig

  @Provides
  def providesSNSConfig(appConfig: CallBackereiConfig) =
    appConfig.snsConfig

  @Provides
  def providesSNSClientConfig(appConfig: CallBackereiConfig) =
    appConfig.snsClientConfig
}
