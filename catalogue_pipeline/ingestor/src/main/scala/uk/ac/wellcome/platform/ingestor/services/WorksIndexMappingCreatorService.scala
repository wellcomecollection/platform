package uk.ac.wellcome.platform.ingestor.services

import com.google.inject.Inject
import uk.ac.wellcome.elasticsearch.{DisplayElasticConfig, WorksIndex}

class WorksIndexMappingCreatorService @Inject()(elasticConfig: DisplayElasticConfig,
                                                worksIndex: WorksIndex) {

  worksIndex.create(elasticConfig.indexV1name)
  worksIndex.create(elasticConfig.indexV2name)

}
