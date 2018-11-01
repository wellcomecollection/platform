package uk.ac.wellcome.platform.archive.archivist.config

import org.rogach.scallop.{ScallopConf, ScallopOption}
import uk.ac.wellcome.messaging.sns.SNSConfig
import uk.ac.wellcome.messaging.sqs.SQSConfig
import uk.ac.wellcome.monitoring.MetricsConfig
import uk.ac.wellcome.platform.archive.archivist.models._
import uk.ac.wellcome.platform.archive.common.config.models.HttpServerConfig
import uk.ac.wellcome.platform.archive.common.modules._

import scala.concurrent.duration._

class ArgsConfigurator(val arguments: Seq[String])
    extends ScallopConf(arguments) {

  private val awsCloudwatchRegion =
    opt[String]("aws-cloudwatch-region", default = Some("eu-west-1"))
  private val awsCloudwatchEndpoint = opt[String]("aws-cloudwatch-endpoint")

  private val metricsNamespace =
    opt[String]("metrics-namespace", default = Some("app"))
  private val metricsFlushIntervalSeconds = opt[Int](
    "metrics-flush-interval-seconds",
    required = false,
    default = Some(20))

  private val appPort =
    opt[Int]("app-port", required = false, default = Some(9001))
  private val appHost =
    opt[String]("app-host", required = false, default = Some("0.0.0.0"))
  private val appBaseUrl = opt[String](
    "app-base-url",
    required = false,
    default = Some("api.wellcomecollection.org/storage/v1"))

  private val awsSnsAccessKey = opt[String]("aws-sns-access-key")
  private val awsSnsSecretKey = opt[String]("aws-sns-secret-key")
  private val awsSnsRegion =
    opt[String]("aws-sns-region", default = Some("eu-west-1"))
  private val awsSnsEndpoint = opt[String]("aws-sns-endpoint")

  private val registrarSnsTopicArn =
    opt[String]("registrar-sns-topic-arn", required = false)
  private val progressSnsTopicArn =
    opt[String]("progress-sns-topic-arn", required = false)

  private val awsSqsAccessKey = opt[String]("aws-sqs-access-key")
  private val awsSqsSecretKey = opt[String]("aws-sqs-secret-key")
  private val awsSqsRegion =
    opt[String]("aws-sqs-region", default = Some("eu-west-1"))
  private val awsSqsEndpoint = opt[String]("aws-sqs-endpoint")

  private val uploadNamespace =
    opt[String]("upload-namespace", required = true)
  private val parallelism = opt[Int]("parallelism", default = Some(10))
  private val uploadPrefix =
    opt[String]("upload-prefix", default = Some("archive"))
  private val digestDelimiterRegexp =
    opt[String]("digest-delimiter-regexp", default = Some(" +"))

  private val sqsQueueUrl: ScallopOption[String] =
    opt[String]("sqs-queue-url", required = false)
  private val sqsParallelism =
    opt[Int]("sqs-parallelism", required = false, default = Some(10))

  private val awsS3AccessKey = opt[String]("aws-s3-access-key")
  private val awsS3SecretKey = opt[String]("aws-s3-secret-key")
  private val awsS3Region =
    opt[String]("aws-s3-region", default = Some("eu-west-1"))
  private val awsS3Endpoint = opt[String]("aws-s3-endpoint")

  verify()

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

  val snsClientConfig = SnsClientConfig(
    accessKey = awsSnsAccessKey.toOption,
    secretKey = awsSnsSecretKey.toOption,
    region = awsSnsRegion(),
    endpoint = awsSnsEndpoint.toOption
  )

  val registrarSnsConfig = SNSConfig(
    topicArn = registrarSnsTopicArn()
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
    parallelism = sqsParallelism()
  )

  val s3ClientConfig = S3ClientConfig(
    accessKey = awsS3AccessKey.toOption,
    secretKey = awsS3SecretKey.toOption,
    region = awsS3Region(),
    endpoint = awsS3Endpoint.toOption
  )

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
