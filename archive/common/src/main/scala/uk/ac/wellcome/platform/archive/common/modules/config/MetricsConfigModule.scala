package uk.ac.wellcome.platform.archive.common.modules.config

import com.google.inject.AbstractModule
import com.typesafe.config.Config
import uk.ac.wellcome.monitoring.MetricsConfig

import scala.concurrent.duration._

object MetricsConfigModule extends AbstractModule {

  import ConfigHelper._

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
