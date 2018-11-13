package uk.ac.wellcome.platform.ingestor.services

import uk.ac.wellcome.elasticsearch.WorksIndex
import uk.ac.wellcome.platform.ingestor.config.models.IngestElasticConfig

class WorksIndexMappingCreatorService(
  elasticConfig: IngestElasticConfig,
  worksIndex: WorksIndex) {

  worksIndex.create(elasticConfig.indexName)

}
