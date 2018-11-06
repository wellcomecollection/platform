package uk.ac.wellcome.platform.archive.notifier.modules

import com.google.inject.{AbstractModule, Provides}
import uk.ac.wellcome.platform.archive.notifier.models.NotifierConfig

object ConfigModule extends AbstractModule {
  @Provides
  def providesCloudwatchClientConfig(appConfig: NotifierConfig) =
    appConfig.cloudwatchClientConfig

  @Provides
  def providesSQSConfig(appConfig: NotifierConfig) =
    appConfig.sqsConfig

  @Provides
  def providesSQSClientConfig(appConfig: NotifierConfig) =
    appConfig.sqsClientConfig

  @Provides
  def providesMetricsConfig(appConfig: NotifierConfig) =
    appConfig.metricsConfig

  @Provides
  def providesContextUrl(appConfig: NotifierConfig) =
    appConfig.contextUrl
}
