package uk.ac.wellcome.platform.archive.notifier.config

import org.rogach.scallop.{ScallopConf, ScallopOption}
import uk.ac.wellcome.messaging.sns.SNSConfig
import uk.ac.wellcome.messaging.sqs.SQSConfig
import uk.ac.wellcome.monitoring.MetricsConfig
import uk.ac.wellcome.platform.archive.common.modules.{CloudwatchClientConfig, SQSClientConfig, SnsClientConfig}
import uk.ac.wellcome.platform.archive.notifier.models.NotifierConfig

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

  verify()

  val appConfig = NotifierConfig(
    cloudwatchClientConfig = cloudwatchClientConfig,
    sqsClientConfig = sqsClientConfig,
    sqsConfig = sqsConfig,
    snsClientConfig = snsClientConfig,
    snsConfig = snsConfig,
    metricsConfig = metricsConfig
  )
}
