package uk.ac.wellcome.platform.archive.registrar.http.modules

import com.google.inject.{AbstractModule, Provides}
import grizzled.slf4j.Logging
import uk.ac.wellcome.platform.archive.registrar.http.config.{RegistrarHttpArgsConfigurator, RegistrarHttpConfig}

class AppConfigModule(val args: Array[String])
    extends AbstractModule
    with Logging {
  debug(s"Application config loaded from args: ${args.toList}")

  override def configure(): Unit = {
    bind(classOf[RegistrarHttpArgsConfigurator])
      .toInstance(new RegistrarHttpArgsConfigurator(args))
  }

  @Provides
  def providesAppConfig(
    configurator: RegistrarHttpArgsConfigurator): RegistrarHttpConfig =
    configurator.appConfig
}
