package uk.ac.wellcome.platform.api.controllers

import com.google.inject.{Inject, Singleton}
import uk.ac.wellcome.display.models.{ApiVersions, V2WorksIncludes}
import uk.ac.wellcome.display.models.v2.DisplayWorkV2
import uk.ac.wellcome.elasticsearch.ElasticConfig
import uk.ac.wellcome.platform.api.models.ApiConfig
import uk.ac.wellcome.platform.api.requests.{
  V2MultipleResultsRequest,
  V2SingleWorkRequest
}
import uk.ac.wellcome.platform.api.services.WorksService

import scala.concurrent.ExecutionContext

@Singleton
class V2WorksController @Inject()(
  apiConfig: ApiConfig,
  elasticConfig: ElasticConfig,
  worksService: WorksService)(implicit ec: ExecutionContext)
    extends WorksController[
      V2MultipleResultsRequest,
      V2SingleWorkRequest,
      V2WorksIncludes](
      apiConfig = apiConfig,
      indexName = elasticConfig.indexV2name,
      worksService = worksService
    ) {
  implicit protected val swagger = ApiV2Swagger

  override def emptyWorksIncludes: V2WorksIncludes = V2WorksIncludes.apply()
  override def recognisedIncludes: List[String] =
    V2WorksIncludes.recognisedIncludes
  lazy override protected val includeParameterName: String = "include"

  prefix(s"${apiConfig.pathPrefix}/${ApiVersions.v2.toString}") {
    setupResultListEndpoint(ApiVersions.v2, "/works", DisplayWorkV2.apply)
    setupSingleWorkEndpoint(ApiVersions.v2, "/works/:id", DisplayWorkV2.apply)
  }

}
