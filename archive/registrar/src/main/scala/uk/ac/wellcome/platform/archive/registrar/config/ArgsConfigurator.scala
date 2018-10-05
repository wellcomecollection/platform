package uk.ac.wellcome.platform.archive.registrar.config

import org.rogach.scallop.ScallopConf
import uk.ac.wellcome.platform.archive.common.config._
import uk.ac.wellcome.platform.archive.registrar.models.RegistrarConfig

class ArgsConfigurator(val arguments: Seq[String])
    extends ScallopConf(arguments)
    with CloudWatchClientConfigurator
    with MetricsConfigConfigurator
    with HttpServerConfigurator
    with SnsClientConfigurator
    with SnsConfigConfigurator
    with SqsClientConfigurator
    with SqsConfigConfigurator
    with S3ClientConfigurator
    with HybridStoreConfigurator
    with ProgressMonitorConfigurator {

  val uploadNamespace = opt[String](required = true)
  val uploadPrefix = opt[String](default = Some("archive"))
  val digestDelimiterRegexp = opt[String](default = Some(" +"))

  verify()

  val appConfig = RegistrarConfig(
    s3ClientConfig,
    cloudwatchClientConfig,
    sqsClientConfig,
    sqsConfig,
    snsClientConfig,
    snsConfig,
    hybridStoreConfig,
    archiveProgressMonitorConfig,
    metricsConfig
  )
}
