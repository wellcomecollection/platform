package uk.ac.wellcome.platform.ingestor.modules

import com.google.inject.Provides
import com.twitter.app.Flaggable
import com.twitter.inject.TwitterModule
import uk.ac.wellcome.platform.ingestor.IngestElasticConfig
import uk.ac.wellcome.platform.ingestor.config.models.{IngestElasticConfig, IngestorConfig}

import scala.concurrent.duration.{Duration, FiniteDuration, _}

object IngestorConfigModule extends TwitterModule {

  implicit val finiteDurationFlaggable =
    Flaggable.mandatory[FiniteDuration](config =>
      Duration.apply(config).asInstanceOf[FiniteDuration])

  val flushInterval = flag[FiniteDuration](
    "es.ingest.flushInterval",
    1 minute,
    "Interval within which works get ingested into Elasticsearch."
  )

  val batchSize = flag[Int](
    "es.ingest.batchSize",
    100,
    "Maximum size of a batch that gets sent to Elasticsearch"
  )

  @Provides
  def providesIngestorConfig(elasticConfig: IngestElasticConfig) =
    IngestorConfig(batchSize(), flushInterval(), elasticConfig)
}
