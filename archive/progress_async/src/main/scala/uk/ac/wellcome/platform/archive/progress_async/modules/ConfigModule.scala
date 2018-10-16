package uk.ac.wellcome.platform.archive.progress_async.modules

import com.google.inject.{AbstractModule, Provides}
import uk.ac.wellcome.platform.archive.progress_async.models.ProgressAsyncConfig

object ConfigModule extends AbstractModule {

  @Provides
  def providesCloudwatchClientConfig(appConfig: ProgressAsyncConfig) =
    appConfig.cloudwatchClientConfig

  @Provides
  def providesSQSConfig(appConfig: ProgressAsyncConfig) =
    appConfig.sqsConfig

  @Provides
  def providesSQSClientConfig(appConfig: ProgressAsyncConfig) =
    appConfig.sqsClientConfig

  @Provides
  def providesSNSConfig(appConfig: ProgressAsyncConfig) =
    appConfig.snsConfig

  @Provides
  def providesSNSClientConfig(appConfig: ProgressAsyncConfig) =
    appConfig.snsClientConfig

  @Provides
  def providesMetricsConfig(appConfig: ProgressAsyncConfig) =
    appConfig.metricsConfig

  @Provides
  def providesProgressTrackerConfig(appConfig: ProgressAsyncConfig) =
    appConfig.progressTrackerConfig
}
