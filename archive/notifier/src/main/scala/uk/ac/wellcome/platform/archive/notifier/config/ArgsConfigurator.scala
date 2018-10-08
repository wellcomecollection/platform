package uk.ac.wellcome.platform.archive.notifier.config

import org.rogach.scallop.{ScallopConf, ScallopOption}
import uk.ac.wellcome.messaging.sns.SNSConfig
import uk.ac.wellcome.messaging.sqs.SQSConfig
import uk.ac.wellcome.monitoring.MetricsConfig
import uk.ac.wellcome.platform.archive.common.modules.{
  CloudwatchClientConfig,
  SQSClientConfig,
  SnsClientConfig
}
import uk.ac.wellcome.platform.archive.notifier.models.NotifierConfig

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

  private val awsSnsAccessKey = opt[String]("aws-sns-access-key")
  private val awsSnsSecretKey = opt[String]("aws-sns-secret-key")
  private val awsSnsRegion =
    opt[String]("aws-sns-region", default = Some("eu-west-1"))
  private val awsSnsEndpoint = opt[String]("aws-sns-endpoint")

  private val snsTopicArn: ScallopOption[String] =
    opt[String]("sns-topic-arn", required = true)

  private val awsSqsAccessKey = opt[String]("aws-sqs-access-key")
  private val awsSqsSecretKey = opt[String]("aws-sqs-secret-key")
  private val awsSqsRegion =
    opt[String]("aws-sqs-region", default = Some("eu-west-1"))
  private val awsSqsEndpoint = opt[String]("aws-sqs-endpoint")

  private val sqsQueueUrl: ScallopOption[String] =
    opt[String]("sqs-queue-url", required = true)
  private val sqsWaitTimeSeconds =
    opt[Int]("sqs-wait-time-seconds", required = true, default = Some(20))
  private val sqsMaxMessages =
    opt[Int]("sqs-max-messages", required = true, default = Some(10))
  private val sqsParallelism =
    opt[Int]("sqs-parallelism", required = true, default = Some(10))

  verify()

  val snsConfig = SNSConfig(
    topicArn = snsTopicArn()
  )

  val snsClientConfig = SnsClientConfig(
    accessKey = awsSnsAccessKey.toOption,
    secretKey = awsSnsSecretKey.toOption,
    region = awsSnsRegion(),
    endpoint = awsSnsEndpoint.toOption
  )

  val metricsConfig = MetricsConfig(
    namespace = metricsNamespace(),
    flushInterval = metricsFlushIntervalSeconds() seconds
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
    queueUrl = sqsQueueUrl(),
    waitTime = sqsWaitTimeSeconds() seconds,
    maxMessages = sqsMaxMessages(),
    parallelism = sqsParallelism()
  )

  val appConfig = NotifierConfig(
    cloudwatchClientConfig = cloudwatchClientConfig,
    sqsClientConfig = sqsClientConfig,
    sqsConfig = sqsConfig,
    snsClientConfig = snsClientConfig,
    snsConfig = snsConfig,
    metricsConfig = metricsConfig
  )
}
