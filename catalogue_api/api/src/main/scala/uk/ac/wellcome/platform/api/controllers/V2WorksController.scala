package uk.ac.wellcome.platform.api.controllers

import com.twitter.inject.annotations.Flag
import javax.inject.{Inject, Singleton}
import uk.ac.wellcome.display.models.v2.DisplayWorkV2
import uk.ac.wellcome.platform.api.services.WorksService
import uk.ac.wellcome.versions.ApiVersions

@Singleton
class V2WorksController @Inject()(
  @Flag("api.prefix") apiPrefix: String,
  @Flag("api.context.suffix") apiContextSuffix: String,
  @Flag("api.host") apiHost: String,
  @Flag("api.scheme") apiScheme: String,
  @Flag("api.pageSize") defaultPageSize: Int,
  @Flag("es.index.v2") indexName: String,
  worksService: WorksService)
    extends WorksController(
      apiPrefix = apiPrefix,
      apiContextSuffix = apiContextSuffix,
      apiHost = apiHost,
      apiScheme = apiScheme,
      indexName = indexName,
      defaultPageSize = defaultPageSize,
      worksService = worksService
    ) {
  implicit protected val swagger = ApiV2Swagger

  prefix(s"$apiPrefix/${ApiVersions.v2.toString}") {
    setupResultListEndpoint(ApiVersions.v2, "/works", DisplayWorkV2.apply)
    setupSingleWorkEndpoint(ApiVersions.v2, "/works/:id", DisplayWorkV2.apply)
  }
}
