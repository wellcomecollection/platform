package uk.ac.wellcome.platform.archive.registrar.config

import org.rogach.scallop.{ScallopConf, ScallopOption}
import uk.ac.wellcome.messaging.sns.SNSConfig
import uk.ac.wellcome.messaging.sqs.SQSConfig
import uk.ac.wellcome.monitoring.MetricsConfig
import uk.ac.wellcome.platform.archive.common.models.HttpServerConfig
import uk.ac.wellcome.platform.archive.common.modules._
import uk.ac.wellcome.platform.archive.common.progress.modules.ProgressMonitorConfig
import uk.ac.wellcome.platform.archive.registrar.models.RegistrarConfig
import uk.ac.wellcome.platform.archive.registrar.modules.HybridStoreConfig
import uk.ac.wellcome.storage.dynamo.DynamoConfig
import uk.ac.wellcome.storage.s3.S3Config

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

  private val awsSnsAccessKey = opt[String]()
  private val awsSnsSecretKey = opt[String]()
  private val awsSnsRegion = opt[String](default = Some("eu-west-1"))
  private val awsSnsEndpoint = opt[String]()

  val snsClientConfig = SnsClientConfig(
    accessKey = awsSnsAccessKey.toOption,
    secretKey = awsSnsSecretKey.toOption,
    region = awsSnsRegion(),
    endpoint = awsSnsEndpoint.toOption
  )

  private val snsTopicArn: ScallopOption[String] = opt[String](required = true)

  val snsConfig = SNSConfig(
    topicArn = snsTopicArn()
  )

  private val awsSqsAccessKey = opt[String]()
  private val awsSqsSecretKey = opt[String]()
  private val awsSqsRegion = opt[String](default = Some("eu-west-1"))
  private val awsSqsEndpoint = opt[String]()

  val sqsClientConfig = SQSClientConfig(
    accessKey = awsSqsAccessKey.toOption,
    secretKey = awsSqsSecretKey.toOption,
    region = awsSqsRegion(),
    endpoint = awsSqsEndpoint.toOption
  )

  private val sqsQueueUrl: ScallopOption[String] = opt[String](required = true)
  private val sqsWaitTimeSeconds = opt[Int](required = true, default = Some(20))
  private val sqsMaxMessages = opt[Int](required = true, default = Some(10))
  private val sqsParallelism = opt[Int](required = true, default = Some(10))

  val sqsConfig = SQSConfig(
    queueUrl = sqsQueueUrl(),
    waitTime = sqsWaitTimeSeconds() seconds,
    maxMessages = sqsMaxMessages(),
    parallelism = sqsParallelism()
  )

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

  private val awsS3AccessKey = opt[String]()
  private val awsS3SecretKey = opt[String]()
  private val awsS3Region = opt[String](default = Some("eu-west-1"))
  private val awsS3Endpoint = opt[String]()

  val s3ClientConfig = S3ClientConfig(
    accessKey = awsS3AccessKey.toOption,
    secretKey = awsS3SecretKey.toOption,
    region = awsS3Region(),
    endpoint = awsS3Endpoint.toOption
  )

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
    snsConfig,
    hybridStoreConfig,
    archiveProgressMonitorConfig,
    metricsConfig
  )
}
