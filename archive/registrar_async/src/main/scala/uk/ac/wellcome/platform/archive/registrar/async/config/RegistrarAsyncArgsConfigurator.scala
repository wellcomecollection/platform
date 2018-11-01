package uk.ac.wellcome.platform.archive.registrar.async.config

import org.rogach.scallop.{ScallopConf, ScallopOption}
import uk.ac.wellcome.messaging.sns.SNSConfig
import uk.ac.wellcome.messaging.sqs.SQSConfig
import uk.ac.wellcome.monitoring.MetricsConfig
import uk.ac.wellcome.platform.archive.common.modules._
import uk.ac.wellcome.platform.archive.registrar.async.models.RegistrarAsyncConfig
import uk.ac.wellcome.platform.archive.registrar.common.modules.HybridStoreConfig
import uk.ac.wellcome.storage.dynamo.DynamoConfig
import uk.ac.wellcome.storage.s3.S3Config

import scala.concurrent.duration._

class RegistrarAsyncArgsConfigurator(val arguments: Seq[String])
    extends ScallopConf(arguments) {

  val hybridDynamoAccessKey = opt[String]("hybrid-dynamo-access-key")
  val hybridDynamoSecretKey = opt[String]("hybrid-dynamo-secret-key")
  val hybridDynamoRegion =
    opt[String]("hybrid-dynamo-region", default = Some("eu-west-1"))
  val hybridDynamoEndpoint = opt[String]("hybrid-dynamo-endpoint")

  val hybridS3AccessKey = opt[String]("hybrid-s3-access-key")
  val hybridS3SecretKey = opt[String]("hybrid-s3-secret-key")
  val hybridS3Region =
    opt[String]("hybrid-s3-region", default = Some("eu-west-1"))
  val hybridS3Endpoint = opt[String]("hybrid-s3-endpoint")

  val hybridGlobalS3Prefix =
    opt[String]("hybrid-global-s3-prefix", default = Some("archive"))
  val hybridDynamoTableName = opt[String]("hybrid-dynamo-table-name")
  val hybridS3BucketName = opt[String]("hybrid-s3-bucket-name")

  private val awsS3AccessKey = opt[String]("aws-s3-access-key")
  private val awsS3SecretKey = opt[String]("aws-s3-secret-key")
  private val awsS3Region =
    opt[String]("aws-s3-region", default = Some("eu-west-1"))
  private val awsS3Endpoint = opt[String]("aws-s3-endpoint")

  private val sqsQueueUrl: ScallopOption[String] =
    opt[String]("sqs-queue-url", required = true)
  private val sqsMaxMessages =
    opt[Int]("sqs-max-messages", required = true, default = Some(10))
  private val sqsParallelism =
    opt[Int]("sqs-parallelism", required = true, default = Some(10))

  private val awsSqsAccessKey = opt[String]("aws-sqs-access-key")
  private val awsSqsSecretKey = opt[String]("aws-sqs-secret-key")
  private val awsSqsRegion =
    opt[String]("aws-sqs-region", default = Some("eu-west-1"))
  private val awsSqsEndpoint = opt[String]("aws-sqs-endpoint")

  private val progressSnsTopicArn: ScallopOption[String] =
    opt[String]("progress-sns-topic-arn", required = true)

  private val awsSnsAccessKey = opt[String]("aws-sns-access-key")
  private val awsSnsSecretKey = opt[String]("aws-sns-secret-key")
  private val awsSnsRegion =
    opt[String]("aws-sns-region", default = Some("eu-west-1"))
  private val awsSnsEndpoint = opt[String]("aws-sns-endpoint")

  private val metricsNamespace =
    opt[String]("metrics-namespace", default = Some("app"))
  private val metricsFlushIntervalSeconds =
    opt[Int](
      "metrics-flush-interval-seconds",
      required = true,
      default = Some(20))

  private val awsCloudwatchRegion =
    opt[String]("aws-cloudwatch-region", default = Some("eu-west-1"))
  private val awsCloudwatchEndpoint = opt[String]("aws-cloudwatch-endpoint")

  verify()

  val cloudwatchClientConfig = CloudwatchClientConfig(
    region = awsCloudwatchRegion(),
    endpoint = awsCloudwatchEndpoint.toOption
  )

  val metricsConfig = MetricsConfig(
    namespace = metricsNamespace(),
    flushInterval = metricsFlushIntervalSeconds() seconds
  )

  val snsClientConfig = SnsClientConfig(
    accessKey = awsSnsAccessKey.toOption,
    secretKey = awsSnsSecretKey.toOption,
    region = awsSnsRegion(),
    endpoint = awsSnsEndpoint.toOption
  )

  val progressSnsConfig = SNSConfig(
    topicArn = progressSnsTopicArn()
  )

  val sqsClientConfig = SQSClientConfig(
    accessKey = awsSqsAccessKey.toOption,
    secretKey = awsSqsSecretKey.toOption,
    region = awsSqsRegion(),
    endpoint = awsSqsEndpoint.toOption
  )

  val sqsConfig = SQSConfig(
    queueUrl = sqsQueueUrl(),
    maxMessages = sqsMaxMessages(),
    parallelism = sqsParallelism()
  )

  val s3ClientConfig = S3ClientConfig(
    accessKey = awsS3AccessKey.toOption,
    secretKey = awsS3SecretKey.toOption,
    region = awsS3Region(),
    endpoint = awsS3Endpoint.toOption
  )

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

  val appConfig = RegistrarAsyncConfig(
    s3ClientConfig,
    cloudwatchClientConfig,
    sqsClientConfig,
    sqsConfig,
    snsClientConfig,
    progressSnsConfig,
    hybridStoreConfig,
    metricsConfig
  )
}
