package uk.ac.wellcome.platform.archive.call_backerei.modules

import com.google.inject.{AbstractModule, Provides}
import grizzled.slf4j.Logging
import uk.ac.wellcome.platform.archive.call_backerei.config.ArgsConfigurator
import uk.ac.wellcome.platform.archive.call_backerei.models.CallBackereiConfig

class AppConfigModule(val args: Array[String])
    extends AbstractModule
    with Logging {
  debug(s"Application config loaded from args: ${args.toList}")

  override def configure(): Unit = {
    bind(classOf[ArgsConfigurator]).toInstance(new ArgsConfigurator(args))
  }

  @Provides
  def providesAppConfig(configurator: ArgsConfigurator): CallBackereiConfig =
    configurator.appConfig
}
