package uk.ac.wellcome.platform.archive.call_backerei.modules

import com.google.inject.{AbstractModule, Provides}
import uk.ac.wellcome.platform.archive.call_backerei.models.CallBäckereiConfig

object ConfigModule extends AbstractModule {
  @Provides
  def providesS3ClientConfig(appConfig: CallBäckereiConfig) =
    appConfig.s3ClientConfig

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

  @Provides
  def providesHybridStoreConfig(appConfig: CallBäckereiConfig) =
    appConfig.hybridStoreConfig

  @Provides
  def providesHybridStoreDynamoClientConfig(appConfig: CallBäckereiConfig) =
    appConfig.hybridStoreConfig.dynamoClientConfig

  @Provides
  def providesProgressMonitorConfig(appConfig: CallBäckereiConfig) =
    appConfig.archiveProgressMonitorConfig
}
