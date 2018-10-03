package uk.ac.wellcome.platform.archive.archivist.config

import org.rogach.scallop.ScallopConf
import uk.ac.wellcome.platform.archive.archivist.models._
import uk.ac.wellcome.platform.archive.common.config._
import uk.ac.wellcome.platform.archive.common.modules._
import uk.ac.wellcome.platform.archive.common.progress.modules.ProgressMonitorConfig
import uk.ac.wellcome.storage.dynamo.DynamoConfig

class ArgsConfigurator(val arguments: Seq[String])
    extends ScallopConf(arguments)
    with CloudWatchClientConfigurator
    with MetricsConfigConfigurator
    with HttpServerConfigurator
    with SnsClientConfigurator
    with RegistrarSnsConfigConfigurator
    with ProgressSnsConfigConfigurator
    with SqsClientConfigurator
    with SqsConfigConfigurator
    with S3ClientConfigurator {

  val uploadNamespace = opt[String](required = true)
  val parallelism = opt[Int](default = Some(10))
  val uploadPrefix = opt[String](default = Some("archive"))
  val digestDelimiterRegexp = opt[String](default = Some(" +"))

  val archiveProgressMonitorTableName = opt[String](required = true)

  val archiveProgressMonitorDynamoAccessKey = opt[String]()
  val archiveProgressMonitorDynamoSecretKey = opt[String]()
  val archiveProgressMonitorDynamoRegion =
    opt[String](default = Some("eu-west-1"))
  val archiveProgressMonitorDynamoEndpoint = opt[String]()

  verify()

  val bagUploaderConfig = BagUploaderConfig(
    uploadConfig = UploadConfig(
      uploadNamespace = uploadNamespace(),
      uploadPrefix = uploadPrefix()
    ),
    bagItConfig = BagItConfig(
      digestDelimiterRegexp = digestDelimiterRegexp()
    ),
    parallelism = parallelism()
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

  val appConfig = ArchivistConfig(
    s3ClientConfig,
    bagUploaderConfig,
    cloudwatchClientConfig,
    sqsClientConfig,
    sqsConfig,
    snsClientConfig,
    registrarSnsConfig,
    progressSnsConfig,
    metricsConfig
  )
}
