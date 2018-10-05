package uk.ac.wellcome.platform.archive.common.modules

import com.google.inject.{AbstractModule, Provides, Singleton}
import com.typesafe.config.Config
import uk.ac.wellcome.monitoring.MetricsConfig
import uk.ac.wellcome.platform.archive.common.models.EnrichConfig

import scala.concurrent.duration._

object MetricsConfigModule extends AbstractModule {
  import EnrichConfig._

  @Singleton
  @Provides
  def providesMetricsConfig(config: Config) = {
    val namespace = config
      .get[String]("metrics.namespace")
      .getOrElse("default")

    val flushInterval = config
      .get[Int]("metrics.flush.interval")
      .getOrElse(30)

    MetricsConfig(
      namespace,
      flushInterval seconds
    )
  }
}
