package uk.ac.wellcome.platform.ingestor.modules

import com.google.inject.Provides
import com.sksamuel.elastic4s.http.HttpClient
import com.twitter.inject.{Injector, TwitterModule}
import uk.ac.wellcome.elasticsearch.WorksIndex
import uk.ac.wellcome.platform.ingestor.config.models.IngestElasticConfig
import uk.ac.wellcome.platform.ingestor.services.WorksIndexMappingCreatorService

import scala.concurrent.ExecutionContext

object WorksIndexModule extends TwitterModule {

  override def singletonStartup(injector: Injector) {
    info("Creating/Updating Elasticsearch index")

    injector.instance[WorksIndexMappingCreatorService]
  }

  @Provides
  def provideWorksIndex(client: HttpClient,
                        elasticSearchConfig: IngestElasticConfig)(
    implicit ec: ExecutionContext): WorksIndex = {
    new WorksIndex(
      client = client,
      rootIndexType = elasticSearchConfig.documentType)
  }
}
