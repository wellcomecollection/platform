package uk.ac.wellcome.platform.archive.registrar.modules

import com.google.inject.name.Named
import com.google.inject.{AbstractModule, Provides}
import uk.ac.wellcome.platform.archive.registrar.models.RegistrarConfig

object ConfigModule extends AbstractModule {
  @Provides
  def providesS3ClientConfig(appConfig: RegistrarConfig) =
    appConfig.s3ClientConfig

  @Provides
  def providesCloudwatchClientConfig(appConfig: RegistrarConfig) =
    appConfig.cloudwatchClientConfig

  @Provides
  def providesSQSConfig(appConfig: RegistrarConfig) =
    appConfig.sqsConfig

  @Provides
  def providesSQSClientConfig(appConfig: RegistrarConfig) =
    appConfig.sqsClientConfig

  @Provides
  def providesMetricsConfig(appConfig: RegistrarConfig) =
    appConfig.metricsConfig

  @Provides @Named("ddsSnsConfig")
  def providesDdsSNSConfig(appConfig: RegistrarConfig) =
    appConfig.ddsSnsConfig

  @Provides @Named("progressSnsConfig")
  def providesProgressSNSConfig(appConfig: RegistrarConfig) =
    appConfig.progressSnsConfig

  @Provides
  def providesSNSClientConfig(appConfig: RegistrarConfig) =
    appConfig.snsClientConfig

  @Provides
  def providesHybridStoreConfig(appConfig: RegistrarConfig) =
    appConfig.hybridStoreConfig

  @Provides
  def providesHybridStoreDynamoClientConfig(appConfig: RegistrarConfig) =
    appConfig.hybridStoreConfig.dynamoClientConfig
}
