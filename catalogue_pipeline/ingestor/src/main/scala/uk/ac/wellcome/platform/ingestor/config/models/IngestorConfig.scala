package uk.ac.wellcome.platform.ingestor.config.models

import scala.concurrent.duration.FiniteDuration

case class IngestorConfig(
  batchSize: Int,
  flushInterval: FiniteDuration,
  index: Index
)
