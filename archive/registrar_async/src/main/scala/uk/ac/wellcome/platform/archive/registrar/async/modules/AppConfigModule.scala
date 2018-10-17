package uk.ac.wellcome.platform.archive.registrar.async.modules

import com.google.inject.{AbstractModule, Provides}
import grizzled.slf4j.Logging
import uk.ac.wellcome.platform.archive.registrar.async.config.ArgsConfigurator
class AppConfigModule(val args: Array[String])
    extends AbstractModule
    with Logging {
  debug(s"Application config loaded from args: ${args.toList}")

  override def configure(): Unit = {
    bind(classOf[ArgsConfigurator]).toInstance(new ArgsConfigurator(args))
  }

  @Provides
  def providesAppConfig(configurator: ArgsConfigurator) =
    configurator.appConfig
}
