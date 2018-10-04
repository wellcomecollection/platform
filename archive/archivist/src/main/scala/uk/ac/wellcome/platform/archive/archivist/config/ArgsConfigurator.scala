package uk.ac.wellcome.platform.archive.archivist.config

import org.rogach.scallop.{ScallopConf, ScallopOption}
import uk.ac.wellcome.messaging.sns.SNSConfig
import uk.ac.wellcome.messaging.sqs.SQSConfig
import uk.ac.wellcome.monitoring.MetricsConfig
import uk.ac.wellcome.platform.archive.archivist.models._
import uk.ac.wellcome.platform.archive.common.models.HttpServerConfig
import uk.ac.wellcome.platform.archive.common.modules._
import uk.ac.wellcome.platform.archive.common.progress.modules.ProgressMonitorConfig
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

  private val registrarSnsTopicArn =
    opt[String](required = true)

  val registrarSnsConfig = SNSConfig(
    topicArn = registrarSnsTopicArn()
  )

  private val progressSnsTopicArn =
    opt[String](required = true)

  val progressSnsConfig = SNSConfig(
    topicArn = progressSnsTopicArn()
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
