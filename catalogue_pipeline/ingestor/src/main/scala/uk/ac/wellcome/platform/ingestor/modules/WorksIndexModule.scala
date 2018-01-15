package uk.ac.wellcome.platform.ingestor.modules

import com.twitter.inject.{Injector, TwitterModule}
import uk.ac.wellcome.platform.ingestor.models.WorksIndex

object WorksIndexModule extends TwitterModule {

  override def singletonStartup(injector: Injector) {
    info("Creating/Updating Elasticsearch index")

    val indexCreator = injector.instance[WorksIndex]

    indexCreator.create
  }
}
