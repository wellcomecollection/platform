package uk.ac.wellcome.platform.archiver.modules

import com.google.inject.{AbstractModule, Provides}
import uk.ac.wellcome.platform.archiver.models.AppConfig

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
  def providesSNSConfig(appConfig: AppConfig) =
    appConfig.snsConfig

  @Provides
  def providesMetricsConfig(appConfig: AppConfig) =
    appConfig.metricsConfig

  @Provides
  def providesBagUploaderConfig(appConfig: AppConfig) =
    appConfig.bagUploaderConfig
}
