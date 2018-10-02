package uk.ac.wellcome.platform.archive.call_backerei.models

import uk.ac.wellcome.messaging.sns.SNSConfig
import uk.ac.wellcome.messaging.sqs.SQSConfig
import uk.ac.wellcome.monitoring.MetricsConfig
import uk.ac.wellcome.platform.archive.common.modules.{
  CloudwatchClientConfig,
  SQSClientConfig,
  SnsClientConfig
}

case class CallBackereiConfig(
  cloudwatchClientConfig: CloudwatchClientConfig,
  sqsClientConfig: SQSClientConfig,
  sqsConfig: SQSConfig,
  snsClientConfig: SnsClientConfig,
  snsConfig: SNSConfig,
  metricsConfig: MetricsConfig
)
