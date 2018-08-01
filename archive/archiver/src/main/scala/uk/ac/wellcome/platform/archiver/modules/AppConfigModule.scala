package uk.ac.wellcome.platform.archiver.modules

import com.google.inject.{AbstractModule, Provides}
import grizzled.slf4j.Logging
import uk.ac.wellcome.platform.archiver.config.{AppConfig, ArgsConfigurator}

class AppConfigModule(val args: Array[String]) extends AbstractModule with Logging {
  debug(s"Application config loaded from args: ${args.toList}")

  override def configure(): Unit = {
    bind(classOf[ArgsConfigurator]).toInstance(new ArgsConfigurator(args))
  }

  @Provides
  def providesAppConfig(configurator: ArgsConfigurator)=
    configurator.appConfig

  @Provides
  def providesS3ClientConfig(appConfig: AppConfig)=
    appConfig.s3ClientConfig

  @Provides
  def providesCloudwatchClientConfig(appConfig: AppConfig)=
    appConfig.cloudwatchClientConfig

  @Provides
  def providesSQSConfig(appConfig: AppConfig) =
    appConfig.sqsConfig

  @Provides
  def providesSQSClientConfig(appConfig: AppConfig) =
    appConfig.sqsClientConfig

  @Provides
  def providesMetricsConfig(appConfig: AppConfig) =
    appConfig.metricsConfig
}