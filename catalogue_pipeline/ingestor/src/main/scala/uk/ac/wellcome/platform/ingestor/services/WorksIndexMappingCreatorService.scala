package uk.ac.wellcome.platform.ingestor.services

import com.google.inject.Inject
import uk.ac.wellcome.elasticsearch.{ElasticConfig, WorksIndex}

class WorksIndexMappingCreatorService @Inject()(elasticConfig: ElasticConfig,
                                                worksIndex: WorksIndex) {

  worksIndex.create(elasticConfig.indexV1name)
  worksIndex.create(elasticConfig.indexV2name)

}
