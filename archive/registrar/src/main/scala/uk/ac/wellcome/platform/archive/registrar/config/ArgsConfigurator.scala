package uk.ac.wellcome.platform.archive.registrar.config

import org.rogach.scallop.ScallopConf
import uk.ac.wellcome.platform.archive.common.config._
import uk.ac.wellcome.platform.archive.common.modules._
import uk.ac.wellcome.platform.archive.common.progress.modules.ProgressMonitorConfig
import uk.ac.wellcome.platform.archive.registrar.models.RegistrarConfig
import uk.ac.wellcome.platform.archive.registrar.modules.HybridStoreConfig
import uk.ac.wellcome.storage.dynamo.DynamoConfig
import uk.ac.wellcome.storage.s3.S3Config

class ArgsConfigurator(val arguments: Seq[String])
    extends ScallopConf(arguments)
    with CloudWatchClientConfigurator
    with MetricsConfigConfigurator
    with HttpServerConfigurator
    with SnsClientConfigurator
    with RegistrarSnsConfigConfigurator
    with SqsClientConfigurator
    with SqsConfigConfigurator
    with S3ClientConfigurator {

  val uploadNamespace = opt[String](required = true)
  val uploadPrefix = opt[String](default = Some("archive"))
  val digestDelimiterRegexp = opt[String](default = Some(" +"))

  val hybridDynamoAccessKey = opt[String]()
  val hybridDynamoSecretKey = opt[String]()
  val hybridDynamoRegion = opt[String](default = Some("eu-west-1"))
  val hybridDynamoEndpoint = opt[String]()

  val hybridS3AccessKey = opt[String]()
  val hybridS3SecretKey = opt[String]()
  val hybridS3Region = opt[String](default = Some("eu-west-1"))
  val hybridS3Endpoint = opt[String]()

  val hybridGlobalS3Prefix = opt[String](default = Some("archive"))
  val hybridDynamoTableName = opt[String]()
  val hybridS3BucketName = opt[String]()

  val archiveProgressMonitorTableName = opt[String](required = true)

  val archiveProgressMonitorDynamoAccessKey = opt[String]()
  val archiveProgressMonitorDynamoSecretKey = opt[String]()
  val archiveProgressMonitorDynamoRegion =
    opt[String](default = Some("eu-west-1"))
  val archiveProgressMonitorDynamoEndpoint = opt[String]()

  verify()

  val hybridStoreConfig = HybridStoreConfig(
    dynamoClientConfig = DynamoClientConfig(
      accessKey = hybridDynamoAccessKey.toOption,
      secretKey = hybridDynamoSecretKey.toOption,
      region = hybridDynamoRegion(),
      endpoint = hybridDynamoEndpoint.toOption
    ),
    s3ClientConfig = S3ClientConfig(
      accessKey = hybridS3AccessKey.toOption,
      secretKey = hybridS3SecretKey.toOption,
      region = hybridS3Region(),
      endpoint = hybridS3Endpoint.toOption
    ),
    dynamoConfig = DynamoConfig(
      table = hybridDynamoTableName(),
      maybeIndex = None
    ),
    s3Config = S3Config(
      bucketName = hybridS3BucketName()
    ),
    s3GlobalPrefix = hybridGlobalS3Prefix()
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

  val appConfig = RegistrarConfig(
    s3ClientConfig,
    cloudwatchClientConfig,
    sqsClientConfig,
    sqsConfig,
    snsClientConfig,
    registrarSnsConfig,
    hybridStoreConfig,
    archiveProgressMonitorConfig,
    metricsConfig
  )
}
