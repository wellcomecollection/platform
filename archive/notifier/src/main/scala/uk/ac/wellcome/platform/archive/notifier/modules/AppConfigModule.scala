package uk.ac.wellcome.platform.archive.notifier.modules

import com.google.inject.{AbstractModule, Provides}
import grizzled.slf4j.Logging
import uk.ac.wellcome.platform.archive.notifier.config.ArgsConfigurator
import uk.ac.wellcome.platform.archive.notifier.models.NotifierConfig

class AppConfigModule(val args: Array[String])
    extends AbstractModule
    with Logging {
  debug(s"Application config loaded from args: ${args.toList}")

  override def configure(): Unit = {
    bind(classOf[ArgsConfigurator]).toInstance(new ArgsConfigurator(args))
  }

  @Provides
  def providesAppConfig(configurator: ArgsConfigurator): NotifierConfig =
    configurator.appConfig
}
