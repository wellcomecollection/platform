package uk.ac.wellcome.platform.archive.progress_http.config

import org.rogach.scallop.ScallopConf
import uk.ac.wellcome.monitoring.MetricsConfig
import uk.ac.wellcome.platform.archive.common.config.models.HttpServerConfig
import uk.ac.wellcome.platform.archive.common.modules._
import uk.ac.wellcome.platform.archive.common.progress.modules.ProgressMonitorConfig
import uk.ac.wellcome.platform.archive.progress_http.models.ProgressHttpConfig
import uk.ac.wellcome.storage.dynamo.DynamoConfig

import scala.concurrent.duration._

class ArgsConfigurator(val arguments: Seq[String])
    extends ScallopConf(arguments) {

  private val awsCloudwatchRegion =
    opt[String]("aws-cloudwatch-region", default = Some("eu-west-1"))
  private val awsCloudwatchEndpoint = opt[String]("aws-cloudwatch-endpoint")

  private val metricsNamespace =
    opt[String]("metrics-namespace", default = Some("app"))
  private val metricsFlushIntervalSeconds =
    opt[Int](
      "metrics-flush-interval-seconds",
      required = true,
      default = Some(20))

  private val appPort =
    opt[Int]("app-port", required = true, default = Some(9001))
  private val appHost =
    opt[String]("app-host", required = true, default = Some("0.0.0.0"))
  private val appBaseUrl =
    opt[String]("app-base-url", required = true)

  private val archiveProgressMonitorTableName =
    opt[String]("archive-progress-monitor-table-name", required = true)

  private val archiveProgressMonitorDynamoAccessKey =
    opt[String]("archive-progress-monitor-dynamo-access-key")
  private val archiveProgressMonitorDynamoSecretKey =
    opt[String]("archive-progress-monitor-dynamo-secret-key")
  private val archiveProgressMonitorDynamoRegion =
    opt[String](
      "archive-progress-monitor-dynamo-region",
      default = Some("eu-west-1"))
  private val archiveProgressMonitorDynamoEndpoint =
    opt[String]("archive-progress-monitor-dynamo-endpoint")

  verify()

  val cloudwatchClientConfig = CloudwatchClientConfig(
    region = awsCloudwatchRegion(),
    endpoint = awsCloudwatchEndpoint.toOption
  )

  val metricsConfig = MetricsConfig(
    namespace = metricsNamespace(),
    flushInterval = metricsFlushIntervalSeconds() seconds
  )

  val httpServerConfig = HttpServerConfig(
    host = appHost(),
    port = appPort(),
    externalBaseUrl = appBaseUrl(),
  )

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
