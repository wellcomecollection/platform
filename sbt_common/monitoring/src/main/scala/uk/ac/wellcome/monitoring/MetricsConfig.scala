package uk.ac.wellcome.monitoring

import scala.concurrent.duration.FiniteDuration

case class MetricsConfig(
  namespace: String,
  flushInterval: FiniteDuration
)
