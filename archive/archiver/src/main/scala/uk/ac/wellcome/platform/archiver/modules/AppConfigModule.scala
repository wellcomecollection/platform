package uk.ac.wellcome.platform.archiver.modules

import com.google.inject.AbstractModule
import uk.ac.wellcome.platform.archiver.models.AppConfig

class AppConfigModule(val config: AppConfig) extends AbstractModule {
  override def configure(): Unit = {
    bind(classOf[AppConfig]).toInstance(config)
  }
}