package uk.ac.wellcome.platform.archive.common.config

import org.rogach.scallop.ScallopConf
import uk.ac.wellcome.monitoring.MetricsConfig

import scala.concurrent.duration._

trait MetricsConfigConfigurator extends ScallopConf {
  val arguments: Seq[String]

  private val metricsNamespace = opt[String](default = Some("app"))
  private val metricsFlushIntervalSeconds =
    opt[Int](required = true, default = Some(20))

  verify()

  val metricsConfig = MetricsConfig(
    namespace = metricsNamespace(),
    flushInterval = metricsFlushIntervalSeconds() seconds
  )
}
