package uk.ac.wellcome.platform.ingestor.services

import com.google.inject.Inject
import uk.ac.wellcome.elasticsearch.WorksIndex
import uk.ac.wellcome.platform.ingestor.config.models.IngestElasticConfig

class WorksIndexMappingCreatorService @Inject()(
  elasticConfig: IngestElasticConfig,
  worksIndex: WorksIndex) {

  worksIndex.create(elasticConfig.indexName)

}
