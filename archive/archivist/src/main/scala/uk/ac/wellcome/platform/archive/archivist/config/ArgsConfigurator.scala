package uk.ac.wellcome.platform.archive.archivist.config

import org.rogach.scallop.{ScallopConf, ScallopOption}
import uk.ac.wellcome.messaging.sns.SNSConfig
import uk.ac.wellcome.messaging.sqs.SQSConfig
import uk.ac.wellcome.monitoring.MetricsConfig
import uk.ac.wellcome.platform.archive.archivist.models._
import uk.ac.wellcome.platform.archive.common.modules._
import uk.ac.wellcome.platform.archive.common.progress.modules.ProgressMonitorConfig
import uk.ac.wellcome.storage.dynamo.DynamoConfig

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

  val appConfig = ArchivistConfig(
    s3ClientConfig,
    bagUploaderConfig,
    cloudwatchClientConfig,
    sqsClientConfig,
    sqsConfig,
    snsClientConfig,
    snsConfig,
    metricsConfig
  )
}
