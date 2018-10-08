package uk.ac.wellcome.platform.archive.common.modules

import com.google.inject.{AbstractModule, Provides}
import com.typesafe.config.{Config, ConfigFactory}
import javax.inject.Inject

object TypesafeConfigModule extends Configurable {

  @Provides
  @Inject
  def providesConfig(): Config = ConfigFactory.load()

}

trait Configurable extends AbstractModule {
  @Provides
  @Inject
  def providesConfig(): Config
}
