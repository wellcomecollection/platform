package uk.ac.wellcome.platform.api.controllers

import com.twitter.inject.annotations.Flag
import javax.inject.{Inject, Singleton}
import uk.ac.wellcome.display.models.v1.DisplayWorkV1
import uk.ac.wellcome.platform.api.services.WorksService
import uk.ac.wellcome.versions.ApiVersions

@Singleton
class V1WorksController @Inject()(
                                   @Flag("api.prefix") apiPrefix: String,
                                   @Flag("api.context.suffix") apiContextSuffix: String,
                                   @Flag("api.host") apiHost: String,
                                   @Flag("api.scheme") apiScheme: String,
                                   @Flag("api.pageSize") defaultPageSize: Int,
                                   worksService: WorksService) extends WorksController(apiPrefix,apiContextSuffix,apiHost,apiScheme,defaultPageSize,worksService) {
  implicit protected val swagger = ApiV1Swagger

  prefix(s"$apiPrefix/${ApiVersions.v1.toString}") {
    setupResultListEndpoint(ApiVersions.v1,"/works",DisplayWorkV1.apply)
    setupSingleWorkEndpoint(ApiVersions.v1, "/works/:id",DisplayWorkV1.apply)
  }
}
