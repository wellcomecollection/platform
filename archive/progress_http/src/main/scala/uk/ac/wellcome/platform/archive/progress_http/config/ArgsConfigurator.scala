package uk.ac.wellcome.platform.archive.progress_http.config

import org.rogach.scallop.ScallopConf
import uk.ac.wellcome.monitoring.MetricsConfig
import uk.ac.wellcome.platform.archive.common.models.HttpServerConfig
import uk.ac.wellcome.platform.archive.common.modules._
import uk.ac.wellcome.platform.archive.common.progress.modules.ProgressMonitorConfig
import uk.ac.wellcome.platform.archive.progress_http.models.ProgressHttpConfig
import uk.ac.wellcome.storage.dynamo.DynamoConfig

import scala.concurrent.duration._

class ArgsConfigurator(val arguments: Seq[String])
    extends ScallopConf(arguments) {

  private val awsCloudwatchRegion = opt[String](default = Some("eu-west-1"))
  private val awsCloudwatchEndpoint = opt[String]()

  val cloudwatchClientConfig = CloudwatchClientConfig(
    region = awsCloudwatchRegion(),
    endpoint = awsCloudwatchEndpoint.toOption
  )

  private val metricsNamespace = opt[String](default = Some("app"))
  private val metricsFlushIntervalSeconds =
    opt[Int](required = true, default = Some(20))

  val metricsConfig = MetricsConfig(
    namespace = metricsNamespace(),
    flushInterval = metricsFlushIntervalSeconds() seconds
  )

  private val appPort =
    opt[Int](required = true, default = Some(9001))

  private val appHost =
    opt[String](required = true, default = Some("0.0.0.0"))

  private val appBaseUrl =
    opt[String](required = true)

  val httpServerConfig = HttpServerConfig(
    host = appHost(),
    port = appPort(),
    externalBaseUrl = appBaseUrl(),
  )

  val archiveProgressMonitorTableName = opt[String](required = true)

  val archiveProgressMonitorDynamoAccessKey = opt[String]()
  val archiveProgressMonitorDynamoSecretKey = opt[String]()
  val archiveProgressMonitorDynamoRegion =
    opt[String](default = Some("eu-west-1"))
  val archiveProgressMonitorDynamoEndpoint = opt[String]()

  verify()

  val archiveProgressMonitorConfig = ProgressMonitorConfig(
    DynamoConfig(
      table = archiveProgressMonitorTableName(),
      maybeIndex = None
    ),
    DynamoClientConfig(
      accessKey = archiveProgressMonitorDynamoAccessKey.toOption,
      secretKey = archiveProgressMonitorDynamoSecretKey.toOption,
      region = archiveProgressMonitorDynamoRegion(),
      endpoint = archiveProgressMonitorDynamoEndpoint.toOption
    )
  )

  val appConfig = ProgressHttpConfig(
    cloudwatchClientConfig,
    archiveProgressMonitorConfig,
    metricsConfig,
    httpServerConfig
  )
}
