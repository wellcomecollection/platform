package uk.ac.wellcome.platform.archive.progress_async.config

import org.rogach.scallop.ScallopConf
import uk.ac.wellcome.monitoring.MetricsConfig
import uk.ac.wellcome.platform.archive.common.config._
import uk.ac.wellcome.platform.archive.common.modules._
import uk.ac.wellcome.platform.archive.common.progress.modules.ProgressMonitorConfig
import uk.ac.wellcome.platform.archive.progress_async.models.ProgressAsyncConfig
import uk.ac.wellcome.storage.dynamo.DynamoConfig

class ArgsConfigurator(val arguments: Seq[String])
    extends ScallopConf(arguments)
    with CloudWatchClientConfigurator
    with MetricsConfigConfigurator
    with SnsClientConfigurator
    with SnsConfigConfigurator
    with SqsClientConfigurator
    with SqsConfigConfigurator {

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

  val appConfig = ProgressAsyncConfig(
    cloudwatchClientConfig,
    sqsClientConfig,
    sqsConfig,
    snsClientConfig,
    snsConfig,
    archiveProgressMonitorConfig,
    metricsConfig
  )
}
