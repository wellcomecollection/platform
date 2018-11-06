package uk.ac.wellcome.platform.archive.common.modules

import com.google.inject.{Inject, Provides}
import com.typesafe.config.{Config, ConfigFactory}

object TypesafeConfigModule extends Configurable {
  @Provides
  @Inject
  def providesConfig(): Config = ConfigFactory.load()
}
