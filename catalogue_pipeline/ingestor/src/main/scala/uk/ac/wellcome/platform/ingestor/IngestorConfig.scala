package uk.ac.wellcome.platform.ingestor

import scala.concurrent.duration.FiniteDuration

case class IngestorConfig(batchSize: Int,
                          flushInterval: FiniteDuration,
                          elasticConfig: IngestElasticConfig)

case class IngestElasticConfig(
                                documentType: String,
                                indexName: String
                              )
