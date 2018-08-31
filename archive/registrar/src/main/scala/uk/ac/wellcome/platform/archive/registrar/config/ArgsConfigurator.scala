package uk.ac.wellcome.platform.archive.registrar.config

import org.rogach.scallop.{ScallopConf, ScallopOption}
import uk.ac.wellcome.messaging.sns.SNSConfig
import uk.ac.wellcome.messaging.sqs.SQSConfig
import uk.ac.wellcome.monitoring.MetricsConfig
import uk.ac.wellcome.platform.archive.common.modules._
import uk.ac.wellcome.platform.archive.common.progress.modules.ArchiveProgressMonitorConfig
import uk.ac.wellcome.platform.archive.registrar.models.RegistrarConfig
import uk.ac.wellcome.platform.archive.registrar.modules.HybridStoreConfig
import uk.ac.wellcome.storage.dynamo.DynamoConfig
import uk.ac.wellcome.storage.s3.S3Config

import scala.concurrent.duration._

class ArgsConfigurator(arguments: Seq[String]) extends ScallopConf(arguments) {

  val awsS3AccessKey = opt[String]()
  val awsS3SecretKey = opt[String]()
  val awsS3Region = opt[String](default = Some("eu-west-1"))
  val awsS3Endpoint = opt[String]()

  val awsSqsAccessKey = opt[String]()
  val awsSqsSecretKey = opt[String]()
  val awsSqsRegion = opt[String](default = Some("eu-west-1"))
  val awsSqsEndpoint = opt[String]()

  val awsSnsAccessKey = opt[String]()
  val awsSnsSecretKey = opt[String]()
  val awsSnsRegion = opt[String](default = Some("eu-west-1"))
  val awsSnsEndpoint = opt[String]()

  val awsCloudwatchRegion = opt[String](default = Some("eu-west-1"))
  val awsCloudwatchEndpoint = opt[String]()

  val topicArn: ScallopOption[String] = opt[String](required = true)
  val sqsQueueUrl: ScallopOption[String] = opt[String](required = true)
  val sqsWaitTimeSeconds = opt[Int](required = true, default = Some(20))
  val sqsMaxMessages = opt[Int](required = true, default = Some(10))
  val sqsParallelism = opt[Int](required = true, default = Some(10))

  val metricsNamespace = opt[String](default = Some("app"))
  val metricsFlushIntervalSeconds =
    opt[Int](required = true, default = Some(20))

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
  val archiveProgressMonitorDynamoRegion = opt[String](default = Some("eu-west-1"))
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

  val s3ClientConfig = S3ClientConfig(
    accessKey = awsS3AccessKey.toOption,
    secretKey = awsS3SecretKey.toOption,
    region = awsS3Region(),
    endpoint = awsS3Endpoint.toOption
  )

  val cloudwatchClientConfig = CloudwatchClientConfig(
    region = awsCloudwatchRegion(),
    endpoint = awsCloudwatchEndpoint.toOption
  )

  val sqsClientConfig = SQSClientConfig(
    accessKey = awsSqsAccessKey.toOption,
    secretKey = awsSqsSecretKey.toOption,
    region = awsSqsRegion(),
    endpoint = awsSqsEndpoint.toOption
  )

  val sqsConfig = SQSConfig(
    sqsQueueUrl(),
    sqsWaitTimeSeconds() seconds,
    sqsMaxMessages(),
    sqsParallelism()
  )

  val snsClientConfig = SnsClientConfig(
    accessKey = awsSnsAccessKey.toOption,
    secretKey = awsSnsSecretKey.toOption,
    region = awsSnsRegion(),
    endpoint = awsSnsEndpoint.toOption
  )

  val snsConfig = SNSConfig(
    topicArn(),
  )

  val metricsConfig = MetricsConfig(
    namespace = metricsNamespace(),
    flushInterval = metricsFlushIntervalSeconds() seconds
  )

  val archiveProgressMonitorConfig = ArchiveProgressMonitorConfig(
    DynamoConfig(
      table = archiveProgressMonitorTableName(),
      maybeIndex = None
    ),
    DynamoClientConfig(
      accessKey = archiveProgressMonitorDynamoAccessKey.toOption,
      secretKey = archiveProgressMonitorDynamoSecretKey.toOption,
      region = archiveProgressMonitorDynamoRegion(),
      endpoint = archiveProgressMonitorDynamoEndpoint.toOption
    ))

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
