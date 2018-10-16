package uk.ac.wellcome.platform.archive.progress_async.models

import uk.ac.wellcome.messaging.sns.SNSConfig
import uk.ac.wellcome.messaging.sqs.SQSConfig
import uk.ac.wellcome.monitoring.MetricsConfig
import uk.ac.wellcome.platform.archive.common.modules._
import uk.ac.wellcome.platform.archive.common.progress.modules.ProgressTrackerConfig

case class ProgressAsyncConfig(
                                cloudwatchClientConfig: CloudwatchClientConfig,
                                sqsClientConfig: SQSClientConfig,
                                sqsConfig: SQSConfig,
                                snsClientConfig: SnsClientConfig,
                                snsConfig: SNSConfig,
                                progressTrackerConfig: ProgressTrackerConfig,
                                metricsConfig: MetricsConfig
)
