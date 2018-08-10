package uk.ac.wellcome.platform.archiver.models

import uk.ac.wellcome.messaging.sqs.SQSConfig
import uk.ac.wellcome.monitoring.MetricsConfig

case class AppConfig(
  s3ClientConfig: S3ClientConfig,
  bagUploaderConfig: BagUploaderConfig,
  cloudwatchClientConfig: CloudwatchClientConfig,
  sqsClientConfig: SQSClientConfig,
  sqsConfig: SQSConfig,
  metricsConfig: MetricsConfig
)

case class S3ClientConfig(
  accessKey: Option[String],
  secretKey: Option[String],
  endpoint: Option[String],
  region: String
)

case class SQSClientConfig(
  accessKey: Option[String],
  secretKey: Option[String],
  endpoint: Option[String],
  region: String
)

case class CloudwatchClientConfig(
  endpoint: Option[String],
  region: String
)

case class BagUploaderConfig(
  uploadNamespace: String,
  uploadPrefix: String = "archive",
  digestDelimiterRegexp: String = " +",
  digestNames: List[String] =
    List("tagmanifest-sha256.txt", "manifest-sha256.txt"))

