package uk.ac.wellcome.platform.registrar.config

import org.rogach.scallop.{ScallopConf, ScallopOption}
import uk.ac.wellcome.messaging.sqs.SQSConfig
import uk.ac.wellcome.monitoring.MetricsConfig
import uk.ac.wellcome.platform.registrar.models._

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

  val awsCloudwatchRegion = opt[String](default = Some("eu-west-1"))
  val awsCloudwatchEndpoint = opt[String]()

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

  verify()

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

  val metricsConfig = MetricsConfig(
    namespace = metricsNamespace(),
    flushInterval = metricsFlushIntervalSeconds() seconds
  )

  val appConfig = AppConfig(
    s3ClientConfig,
    cloudwatchClientConfig,
    sqsClientConfig,
    sqsConfig,
    metricsConfig
  )
}