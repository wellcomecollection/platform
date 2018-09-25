package uk.ac.wellcome.platform.archive.progress.models

import uk.ac.wellcome.messaging.sns.SNSConfig
import uk.ac.wellcome.messaging.sqs.SQSConfig
import uk.ac.wellcome.monitoring.MetricsConfig
import uk.ac.wellcome.platform.archive.common.modules._
import uk.ac.wellcome.platform.archive.common.progress.modules.ProgressMonitorConfig

case class ProgressConfig(
  cloudwatchClientConfig: CloudwatchClientConfig,
  sqsClientConfig: SQSClientConfig,
  sqsConfig: SQSConfig,
  snsClientConfig: SnsClientConfig,
  snsConfig: SNSConfig,
  progressMonitorConfig: ProgressMonitorConfig,
  metricsConfig: MetricsConfig
)
