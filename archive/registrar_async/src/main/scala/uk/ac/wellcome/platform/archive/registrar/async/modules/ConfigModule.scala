package uk.ac.wellcome.platform.archive.registrar.async.modules

import com.google.inject.name.Named
import com.google.inject.{AbstractModule, Provides}
import uk.ac.wellcome.platform.archive.registrar.async.models.RegistrarAsyncConfig

object ConfigModule extends AbstractModule {
  @Provides
  def providesS3ClientConfig(appConfig: RegistrarAsyncConfig) =
    appConfig.s3ClientConfig

  @Provides
  def providesCloudwatchClientConfig(appConfig: RegistrarAsyncConfig) =
    appConfig.cloudwatchClientConfig

  @Provides
  def providesSQSConfig(appConfig: RegistrarAsyncConfig) =
    appConfig.sqsConfig

  @Provides
  def providesSQSClientConfig(appConfig: RegistrarAsyncConfig) =
    appConfig.sqsClientConfig

  @Provides
  def providesMetricsConfig(appConfig: RegistrarAsyncConfig) =
    appConfig.metricsConfig

  @Provides @Named("ddsSnsConfig")
  def providesDdsSNSConfig(appConfig: RegistrarAsyncConfig) =
    appConfig.ddsSnsConfig

  @Provides @Named("progressSnsConfig")
  def providesProgressSNSConfig(appConfig: RegistrarAsyncConfig) =
    appConfig.progressSnsConfig

  @Provides
  def providesSNSClientConfig(appConfig: RegistrarAsyncConfig) =
    appConfig.snsClientConfig

  @Provides
  def providesHybridStoreConfig(appConfig: RegistrarAsyncConfig) =
    appConfig.hybridStoreConfig

  @Provides
  def providesHybridStoreDynamoClientConfig(appConfig: RegistrarAsyncConfig) =
    appConfig.hybridStoreConfig.dynamoClientConfig
}
