package uk.ac.wellcome.platform.archive.progress_http.config

import org.rogach.scallop.ScallopConf
import uk.ac.wellcome.monitoring.MetricsConfig
import uk.ac.wellcome.platform.archive.common.modules._
import uk.ac.wellcome.platform.archive.common.progress.modules.ProgressMonitorConfig
import uk.ac.wellcome.platform.archive.progress_http.models.{HttpServerConfig, ProgressHttpConfig}
import uk.ac.wellcome.storage.dynamo.DynamoConfig

import scala.concurrent.duration._

class ArgsConfigurator(arguments: Seq[String]) extends ScallopConf(arguments) {

  val awsCloudwatchRegion = opt[String](default = Some("eu-west-1"))
  val awsCloudwatchEndpoint = opt[String]()

  val metricsNamespace = opt[String](default = Some("app"))
  val metricsFlushIntervalSeconds =
    opt[Int](required = true, default = Some(20))

  val archiveProgressMonitorTableName = opt[String](required = true)

  val archiveProgressMonitorDynamoAccessKey = opt[String]()
  val archiveProgressMonitorDynamoSecretKey = opt[String]()
  val archiveProgressMonitorDynamoRegion =
    opt[String](default = Some("eu-west-1"))
  val archiveProgressMonitorDynamoEndpoint = opt[String]()

  val appPort =
    opt[Int](required = true, default = Some(9001))

  val appHost =
    opt[String](required = true, default = Some("localhost"))

  verify()

  val cloudwatchClientConfig = CloudwatchClientConfig(
    region = awsCloudwatchRegion(),
    endpoint = awsCloudwatchEndpoint.toOption
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

  val metricsConfig = MetricsConfig(
    namespace = metricsNamespace(),
    flushInterval = metricsFlushIntervalSeconds() seconds
  )

  val httpServerConfig = HttpServerConfig(
    host = appHost(),
    port = appPort()
  )

  val appConfig = ProgressHttpConfig(
    cloudwatchClientConfig,
    archiveProgressMonitorConfig,
    metricsConfig,
    httpServerConfig
  )
}
