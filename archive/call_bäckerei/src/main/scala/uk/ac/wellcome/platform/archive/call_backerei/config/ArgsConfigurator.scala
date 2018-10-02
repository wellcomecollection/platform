package uk.ac.wellcome.platform.archive.call_backerei.config

import org.rogach.scallop.ScallopConf
import uk.ac.wellcome.platform.archive.call_backerei.models.CallBackereiConfig
import uk.ac.wellcome.platform.archive.common.config._

class ArgsConfigurator(val arguments: Seq[String])
    extends ScallopConf(arguments)
    with CloudWatchClientConfigurator
    with MetricsConfigConfigurator
    with SnsClientConfigurator
    with SnsConfigConfigurator
    with SqsClientConfigurator
    with SqsConfigConfigurator {

  verify()

  val appConfig = CallBackereiConfig(
    cloudwatchClientConfig = cloudwatchClientConfig,
    sqsClientConfig = sqsClientConfig,
    sqsConfig = sqsConfig,
    snsClientConfig = snsClientConfig,
    snsConfig = snsConfig,
    metricsConfig = metricsConfig
  )
}
