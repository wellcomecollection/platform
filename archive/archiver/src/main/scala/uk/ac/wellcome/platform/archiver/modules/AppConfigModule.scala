package uk.ac.wellcome.platform.archiver.modules

import com.google.inject.{AbstractModule, Provides}
import uk.ac.wellcome.platform.archiver.models.AppConfig

class AppConfigModule(val args: Array[String]) extends AbstractModule {
  override def configure(): Unit = {
    bind(classOf[AppConfig]).toInstance(new AppConfig(args))
  }

  @Provides
  def providesS3ClientConfig(appConfig: AppConfig)=
    appConfig.s3ClientConfig

  @Provides
  def providesCloudwatchClientConfig(appConfig: AppConfig)=
    appConfig.cloudwatchClientConfig

  @Provides
  def providesSQSConfig(appConfig: AppConfig) =
    appConfig.sqsConfig

}