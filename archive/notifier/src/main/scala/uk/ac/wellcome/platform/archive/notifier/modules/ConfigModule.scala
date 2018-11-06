package uk.ac.wellcome.platform.archive.notifier.modules

import com.google.inject.{AbstractModule, Provides}
import uk.ac.wellcome.platform.archive.notifier.models.NotifierConfig

object ConfigModule extends AbstractModule {
  @Provides
  def providesMetricsConfig(appConfig: NotifierConfig) =
    appConfig.metricsConfig

  @Provides
  def providesContextUrl(appConfig: NotifierConfig) =
    appConfig.contextUrl
}
