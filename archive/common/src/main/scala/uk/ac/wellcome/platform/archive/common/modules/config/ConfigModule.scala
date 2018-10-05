package uk.ac.wellcome.platform.archive.common.modules.config

import com.google.inject.{AbstractModule, Provides}
import com.typesafe.config.{Config, ConfigFactory}
import javax.inject.Inject

object ConfigModule extends AbstractModule {

  @Provides
  @Inject
  def providesConfig(): Config = ConfigFactory.load()

}
