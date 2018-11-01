package uk.ac.wellcome.platform.archive.progress_http.config

import org.rogach.scallop.ScallopConf
import uk.ac.wellcome.messaging.sns.SNSConfig
import uk.ac.wellcome.monitoring.MetricsConfig
import uk.ac.wellcome.platform.archive.common.config.models.HttpServerConfig
import uk.ac.wellcome.platform.archive.common.modules._
import uk.ac.wellcome.platform.archive.common.progress.modules.ProgressTrackerConfig
import uk.ac.wellcome.storage.dynamo.DynamoConfig

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

  private val appPort =
    opt[Int]("app-port", required = true, default = Some(9001))
  private val appHost =
    opt[String]("app-host", required = true, default = Some("0.0.0.0"))
  private val appBaseUrl =
    opt[String]("app-base-url", required = true)

  private val archiveProgressTrackerTableName =
    opt[String]("archive-progress-tracker-table-name", required = true)

  private val archiveProgressTrackerDynamoAccessKey =
    opt[String]("archive-progress-tracker-dynamo-access-key")
  private val archiveProgressTrackerDynamoSecretKey =
    opt[String]("archive-progress-tracker-dynamo-secret-key")
  private val archiveProgressTrackerDynamoRegion =
    opt[String](
      "archive-progress-tracker-dynamo-region",
      default = Some("eu-west-1"))
  private val archiveProgressTrackerDynamoEndpoint =
    opt[String]("archive-progress-tracker-dynamo-endpoint")

  private val awsSnsAccessKey = opt[String]("aws-sns-access-key")
  private val awsSnsSecretKey = opt[String]("aws-sns-secret-key")
  private val awsSnsRegion =
    opt[String]("aws-sns-region", default = Some("eu-west-1"))
  private val awsSnsEndpoint = opt[String]("aws-sns-endpoint")

  private val snsTopicArn =
    opt[String]("sns-topic-arn", required = false)

  verify()

  val snsClientConfig = SnsClientConfig(
    accessKey = awsSnsAccessKey.toOption,
    secretKey = awsSnsSecretKey.toOption,
    region = awsSnsRegion(),
    endpoint = awsSnsEndpoint.toOption
  )

  val snsConfig = SNSConfig(
    topicArn = snsTopicArn()
  )

  val cloudwatchClientConfig = CloudwatchClientConfig(
    region = awsCloudwatchRegion(),
    endpoint = awsCloudwatchEndpoint.toOption
  )

  val metricsConfig = MetricsConfig(
    namespace = metricsNamespace(),
    flushInterval = metricsFlushIntervalSeconds() seconds
  )

  val httpServerConfig = HttpServerConfig(
    host = appHost(),
    port = appPort(),
    externalBaseUrl = appBaseUrl(),
  )

  val archiveProgressTrackerConfig = ProgressTrackerConfig(
    DynamoConfig(
      table = archiveProgressTrackerTableName(),
      maybeIndex = None
    ),
    DynamoClientConfig(
      accessKey = archiveProgressTrackerDynamoAccessKey.toOption,
      secretKey = archiveProgressTrackerDynamoSecretKey.toOption,
      region = archiveProgressTrackerDynamoRegion(),
      endpoint = archiveProgressTrackerDynamoEndpoint.toOption
    )
  )

  val appConfig = ProgressHttpConfig(
    cloudwatchClientConfig,
    archiveProgressTrackerConfig,
    metricsConfig,
    httpServerConfig,
    snsClientConfig,
    snsConfig
  )
}
