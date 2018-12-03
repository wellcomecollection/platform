package uk.ac.wellcome.platform.ingestor.config.builders

import com.sksamuel.elastic4s.Index
import com.typesafe.config.Config
import uk.ac.wellcome.config.core.builders.EnrichConfig._
import uk.ac.wellcome.platform.ingestor.config.models.{
  IngestElasticConfig,
  IngestorConfig
}

import scala.concurrent.duration._

object IngestorConfigBuilder {
  def buildIngestorConfig(config: Config): IngestorConfig = {

    // TODO: Work out how to get a Duration from a Typesafe flag.
    val flushInterval = 1 minute

    val batchSize = config.getOrElse[Int]("es.ingest.batchSize")(default = 100)

    val elasticConfig = buildElasticConfig(config)

    IngestorConfig(
      batchSize = batchSize,
      flushInterval = flushInterval,
      elasticConfig = elasticConfig
    )
  }

  def buildElasticConfig(config: Config): IngestElasticConfig = {
    val documentType = config.getOrElse[String]("es.type")(default = "item")
    val indexName = config.required[String]("es.index")

    IngestElasticConfig(
      documentType = documentType,
      index = Index(indexName)
    )
  }
}
