package uk.ac.wellcome.platform.api.controllers

import com.twitter.inject.annotations.Flag
import com.twitter.finagle.http.Request
import com.twitter.finatra.http.Controller
import javax.inject.{Inject, Singleton}
import uk.ac.wellcome.versions.ApiVersions

@Singleton
class ContextController @Inject()(
  @Flag("api.prefix") apiPrefix: String
) extends Controller {

  prefix(apiPrefix) {
    setupContextEndpoint(ApiVersions.v1)
    setupContextEndpoint(ApiVersions.v2)
  }

  private def setupContextEndpoint(version: ApiVersions.Value): Unit = {
    get(s"/${version.toString}/context.json") { _: Request =>
      response.ok.file("context.json")
    }
  }
}
