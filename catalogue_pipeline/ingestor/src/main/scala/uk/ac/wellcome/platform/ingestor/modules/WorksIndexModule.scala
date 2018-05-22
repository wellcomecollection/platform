package uk.ac.wellcome.platform.ingestor.modules

import com.twitter.inject.{Injector, TwitterModule}
import uk.ac.wellcome.platform.ingestor.services.WorksIndexMappingCreatorService

object WorksIndexModule extends TwitterModule {

  override def singletonStartup(injector: Injector) {
    info("Creating/Updating Elasticsearch index")

    injector.instance[WorksIndexMappingCreatorService]
  }
}
