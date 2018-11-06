package uk.ac.wellcome.platform.archive.common.modules

import com.google.inject.{AbstractModule, Inject, Provides}
import com.typesafe.config.Config

trait Configurable extends AbstractModule {
  @Provides
  @Inject
  def providesConfig(): Config
}
