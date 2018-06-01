package uk.ac.wellcome.platform.api.controllers

import javax.inject.{Inject, Singleton}

import uk.ac.wellcome.display.models.ApiVersions
import uk.ac.wellcome.display.models.v1.DisplayWorkV1
import uk.ac.wellcome.elasticsearch.ElasticConfig
import uk.ac.wellcome.platform.api.models.ApiConfig
import uk.ac.wellcome.platform.api.services.WorksService

@Singleton
class V1WorksController @Inject()(
  apiConfig: ApiConfig,
  elasticConfig: ElasticConfig,
  worksService: WorksService)
    extends WorksController(
      apiConfig = apiConfig,
      indexName = elasticConfig.indexV1name,
      worksService = worksService
    ) {
  implicit protected val swagger = ApiV1Swagger

  prefix(s"${apiConfig.pathPrefix}/${ApiVersions.v1.toString}") {
    setupResultListEndpoint(ApiVersions.v1, "/works", DisplayWorkV1.apply)
    setupSingleWorkEndpoint(ApiVersions.v1, "/works/:id", DisplayWorkV1.apply)
  }
}
