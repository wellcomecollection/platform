package uk.ac.wellcome.platform.archive.archivist.modules

import com.google.inject.{AbstractModule, Provides}
import uk.ac.wellcome.platform.archive.archivist.models.ArchivistConfig

object ConfigModule extends AbstractModule {
  @Provides
  def providesS3ClientConfig(appConfig: ArchivistConfig) =
    appConfig.s3ClientConfig

  @Provides
  def providesCloudwatchClientConfig(appConfig: ArchivistConfig) =
    appConfig.cloudwatchClientConfig

  @Provides
  def providesSQSConfig(appConfig: ArchivistConfig) =
    appConfig.sqsConfig

  @Provides
  def providesSQSClientConfig(appConfig: ArchivistConfig) =
    appConfig.sqsClientConfig

  @Provides
  def providesSNSConfig(appConfig: ArchivistConfig) =
    appConfig.snsConfig

  @Provides
  def providesSNSClientConfig(appConfig: ArchivistConfig) =
    appConfig.snsClientConfig

  @Provides
  def providesMetricsConfig(appConfig: ArchivistConfig) =
    appConfig.metricsConfig

  @Provides
  def providesBagUploaderConfig(appConfig: ArchivistConfig) =
    appConfig.bagUploaderConfig
}
