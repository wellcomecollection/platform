package uk.ac.wellcome.platform.archive.progress_http.modules

import com.google.inject.{AbstractModule, Provides}
import uk.ac.wellcome.platform.archive.progress_http.models.ProgressHttpConfig

object ConfigModule extends AbstractModule {
  @Provides
  def providesCloudwatchClientConfig(appConfig: ProgressHttpConfig) =
    appConfig.cloudwatchClientConfig

  @Provides
  def providesMetricsConfig(appConfig: ProgressHttpConfig) =
    appConfig.metricsConfig

  @Provides
  def providesProgressMonitorConfig(appConfig: ProgressHttpConfig) =
    appConfig.progressMonitorConfig

  @Provides
  def providesHttpServerConfig(appConfig: ProgressHttpConfig) =
    appConfig.httpServerConfig
}
