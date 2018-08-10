package uk.ac.wellcome.platform.registrar.modules

import com.google.inject.{AbstractModule, Provides}
import uk.ac.wellcome.platform.registrar.models.AppConfig

object ConfigModule extends AbstractModule {
  @Provides
  def providesS3ClientConfig(appConfig: AppConfig) =
    appConfig.s3ClientConfig

  @Provides
  def providesCloudwatchClientConfig(appConfig: AppConfig) =
    appConfig.cloudwatchClientConfig

  @Provides
  def providesSQSConfig(appConfig: AppConfig) =
    appConfig.sqsConfig

  @Provides
  def providesSQSClientConfig(appConfig: AppConfig) =
    appConfig.sqsClientConfig

  @Provides
  def providesMetricsConfig(appConfig: AppConfig) =
    appConfig.metricsConfig
}
