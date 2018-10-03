package uk.ac.wellcome.platform.archive.notifier.models

import uk.ac.wellcome.messaging.sns.SNSConfig
import uk.ac.wellcome.messaging.sqs.SQSConfig
import uk.ac.wellcome.monitoring.MetricsConfig
import uk.ac.wellcome.platform.archive.common.modules.{
  CloudwatchClientConfig,
  SQSClientConfig,
  SnsClientConfig
}

case class NotifierConfig(
  cloudwatchClientConfig: CloudwatchClientConfig,
  sqsClientConfig: SQSClientConfig,
  sqsConfig: SQSConfig,
  snsClientConfig: SnsClientConfig,
  snsConfig: SNSConfig,
  metricsConfig: MetricsConfig
)
