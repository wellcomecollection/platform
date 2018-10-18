package uk.ac.wellcome.platform.archive.registrar.async.models
import uk.ac.wellcome.messaging.sns.SNSConfig
import uk.ac.wellcome.messaging.sqs.SQSConfig
import uk.ac.wellcome.monitoring.MetricsConfig
import uk.ac.wellcome.platform.archive.common.modules.{
  CloudwatchClientConfig,
  S3ClientConfig,
  SQSClientConfig,
  SnsClientConfig
}
import uk.ac.wellcome.platform.archive.registrar.common.modules.HybridStoreConfig

case class RegistrarAsyncConfig(s3ClientConfig: S3ClientConfig,
                                cloudwatchClientConfig: CloudwatchClientConfig,
                                sqsClientConfig: SQSClientConfig,
                                sqsConfig: SQSConfig,
                                snsClientConfig: SnsClientConfig,
                                ddsSnsConfig: SNSConfig,
                                progressSnsConfig: SNSConfig,
                                hybridStoreConfig: HybridStoreConfig,
                                metricsConfig: MetricsConfig)
