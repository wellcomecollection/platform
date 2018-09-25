package uk.ac.wellcome.platform.archive.progress.modules

import com.google.inject.{AbstractModule, Provides}
import uk.ac.wellcome.platform.archive.progress.models.ProgressConfig

object ConfigModule extends AbstractModule {
  @Provides
  def providesS3ClientConfig(appConfig: ProgressConfig) =
    appConfig.s3ClientConfig

  @Provides
  def providesCloudwatchClientConfig(appConfig: ProgressConfig) =
    appConfig.cloudwatchClientConfig

  @Provides
  def providesSQSConfig(appConfig: ProgressConfig) =
    appConfig.sqsConfig

  @Provides
  def providesSQSClientConfig(appConfig: ProgressConfig) =
    appConfig.sqsClientConfig

  @Provides
  def providesSNSConfig(appConfig: ProgressConfig) =
    appConfig.snsConfig

  @Provides
  def providesSNSClientConfig(appConfig: ProgressConfig) =
    appConfig.snsClientConfig

  @Provides
  def providesMetricsConfig(appConfig: ProgressConfig) =
    appConfig.metricsConfig

  @Provides
  def providesProgressMonitorConfig(appConfig: ProgressConfig) =
    appConfig.progressMonitorConfig
}
