package uk.ac.wellcome.platform.archive.registrar.models

import uk.ac.wellcome.messaging.sns.SNSConfig
import uk.ac.wellcome.messaging.sqs.SQSConfig
import uk.ac.wellcome.monitoring.MetricsConfig
import uk.ac.wellcome.platform.archive.common.config.models.{HybridStoreConfig, ProgressMonitorConfig}
import uk.ac.wellcome.platform.archive.common.modules._

case class RegistrarConfig(s3ClientConfig: S3ClientConfig,
                           cloudwatchClientConfig: CloudwatchClientConfig,
                           sqsClientConfig: SQSClientConfig,
                           sqsConfig: SQSConfig,
                           snsClientConfig: SnsClientConfig,
                           snsConfig: SNSConfig,
                           hybridStoreConfig: HybridStoreConfig,
                           archiveProgressMonitorConfig: ProgressMonitorConfig,
                           metricsConfig: MetricsConfig)
