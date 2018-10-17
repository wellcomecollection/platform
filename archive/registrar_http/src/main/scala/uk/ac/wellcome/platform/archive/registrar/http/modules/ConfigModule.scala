package uk.ac.wellcome.platform.archive.registrar.http.modules

import com.google.inject.{AbstractModule, Provides}
import uk.ac.wellcome.platform.archive.registrar.http.models.RegistrarHttpConfig

object ConfigModule extends AbstractModule {
  @Provides
  def providesHybridStoreConfig(appConfig: RegistrarHttpConfig) =
    appConfig.hybridStoreConfig

  @Provides
  def providesHttpServerConfig(appConfig: RegistrarHttpConfig) =
    appConfig.httpServerConfig
}
