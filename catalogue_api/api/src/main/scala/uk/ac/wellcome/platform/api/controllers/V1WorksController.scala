package uk.ac.wellcome.platform.api.controllers

import com.google.inject.{Inject, Singleton}
import uk.ac.wellcome.display.models.v1.DisplayWorkV1
import uk.ac.wellcome.display.models.{ApiVersions, V1WorksIncludes}
import uk.ac.wellcome.elasticsearch.DisplayElasticConfig
import uk.ac.wellcome.platform.api.models.{ApiConfig, WorkFilter}
import uk.ac.wellcome.platform.api.requests.{
  V1MultipleResultsRequest,
  V1SingleWorkRequest
}
import uk.ac.wellcome.platform.api.services.WorksService

import scala.concurrent.ExecutionContext

@Singleton
class V1WorksController @Inject()(
  apiConfig: ApiConfig,
  elasticConfig: DisplayElasticConfig,
  worksService: WorksService)(implicit ec: ExecutionContext)
    extends WorksController[
      V1MultipleResultsRequest,
      V1SingleWorkRequest,
      V1WorksIncludes](
      apiConfig = apiConfig,
      defaultIndex = elasticConfig.indexV1,
      worksService = worksService
    ) {
  implicit protected val swagger = ApiV1Swagger
  override def emptyWorksIncludes: V1WorksIncludes = V1WorksIncludes.apply()
  override def recognisedIncludes: List[String] =
    V1WorksIncludes.recognisedIncludes

  prefix(s"${apiConfig.pathPrefix}/${ApiVersions.v1.toString}") {
    setupResultListEndpoint(ApiVersions.v1, "/works", DisplayWorkV1.apply)
    setupSingleWorkEndpoint(ApiVersions.v1, "/works/:id", DisplayWorkV1.apply)
  }
  lazy override protected val includeParameterName: String = "includes"

  /* There's no requirement for any filtering in the V1 API. */
  override def buildFilters(
    request: V1MultipleResultsRequest): List[WorkFilter] = List()
}
