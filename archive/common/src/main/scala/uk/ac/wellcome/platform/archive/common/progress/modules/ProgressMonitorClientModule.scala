package uk.ac.wellcome.platform.archive.common.progress.modules

import com.google.inject.{AbstractModule, Provides}
import com.typesafe.config.Config
import javax.inject.Singleton
import uk.ac.wellcome.platform.archive.common.models.EnrichConfig
import uk.ac.wellcome.platform.archive.common.modules.AkkaModule

object ProgressMonitorClientModule extends AbstractModule {
  install(AkkaModule)

  import EnrichConfig._

  @Provides
  @Singleton
  def providesProgressTopic(config: Config) = {
    val arn = config
      .required[String]("progress.topic.arn")

    ProgressTopic(arn)
  }
}

case class ProgressTopic(arn: String)

