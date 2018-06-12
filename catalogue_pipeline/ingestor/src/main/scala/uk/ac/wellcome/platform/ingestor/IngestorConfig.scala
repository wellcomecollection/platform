package uk.ac.wellcome.platform.ingestor

import uk.ac.wellcome.elasticsearch.ElasticConfig

import scala.concurrent.duration.FiniteDuration

case class IngestorConfig(batchSize: Int, flushInterval: FiniteDuration, elasticConfig: ElasticConfig)
