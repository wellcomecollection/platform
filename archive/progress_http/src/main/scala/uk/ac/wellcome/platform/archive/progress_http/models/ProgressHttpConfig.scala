package uk.ac.wellcome.platform.archive.progress_http.models

import uk.ac.wellcome.monitoring.MetricsConfig
import uk.ac.wellcome.platform.archive.common.modules._
import uk.ac.wellcome.platform.archive.common.progress.modules.ProgressMonitorConfig

case class ProgressHttpConfig(
  cloudwatchClientConfig: CloudwatchClientConfig,
  progressMonitorConfig: ProgressMonitorConfig,
  metricsConfig: MetricsConfig,
  httpServerConfig: HttpServerConfig,
)
