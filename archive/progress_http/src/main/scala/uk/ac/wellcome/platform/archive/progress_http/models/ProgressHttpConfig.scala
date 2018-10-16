package uk.ac.wellcome.platform.archive.progress_http.models

import uk.ac.wellcome.monitoring.MetricsConfig
import uk.ac.wellcome.platform.archive.common.config.models.HttpServerConfig
import uk.ac.wellcome.platform.archive.common.modules._
import uk.ac.wellcome.platform.archive.common.progress.modules.ProgressTrackerConfig

case class ProgressHttpConfig(
  cloudwatchClientConfig: CloudwatchClientConfig,
  progressTrackerConfig: ProgressTrackerConfig,
  metricsConfig: MetricsConfig,
  httpServerConfig: HttpServerConfig,
)
