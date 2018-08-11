package uk.ac.wellcome.platform.archiver.models

import uk.ac.wellcome.messaging.sns.SNSConfig
import uk.ac.wellcome.messaging.sqs.SQSConfig
import uk.ac.wellcome.monitoring.MetricsConfig

case class AppConfig(
                      s3ClientConfig: S3ClientConfig,
                      bagUploaderConfig: BagUploaderConfig,
                      cloudwatchClientConfig: CloudwatchClientConfig,
                      sqsClientConfig: SQSClientConfig,
                      sqsConfig: SQSConfig,
                      snsClientConfig: SnsClientConfig,
                      snsConfig: SNSConfig,
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

case class SnsClientConfig(
                            accessKey: Option[String],
                            secretKey: Option[String],
                            endpoint: Option[String],
                            region: String
                          )

case class CloudwatchClientConfig(
                                   endpoint: Option[String],
                                   region: String
                                 )

case class UploadConfig(uploadNamespace: String,
                        uploadPrefix: String = "archive")

case class BagItConfig(digestDelimiterRegexp: String = " +",
                       tagManifestFilePattern: String = "tagmanifest-%s.txt",
                       manifestFilePattern: String = "manifest-%s.txt",
                       algorithm: String = "sha256"
                      ) {

  def tagManifestFileName =
    tagManifestFilePattern.format(algorithm)

  def manifestFileName =
    manifestFilePattern.format(algorithm)

  def digestNames = List(tagManifestFileName, manifestFileName)
}

case class BagUploaderConfig(
                              uploadConfig: UploadConfig,
                              bagItConfig: BagItConfig = BagItConfig()
                            )

