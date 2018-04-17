package uk.ac.wellcome.platform.ingestor.services

import com.google.inject.Inject
import com.twitter.inject.annotations.Flag
import uk.ac.wellcome.elasticsearch.WorksIndex

class WorksIndexMappingCreatorService@Inject()(
                                                @Flag("es.index.v1") esIndexV1: String,@Flag("es.index.v2") esIndexV2: String,worksIndex: WorksIndex){
  worksIndex.create(esIndexV1)
  worksIndex.create(esIndexV2)

}
