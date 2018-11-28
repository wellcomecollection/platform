package uk.ac.wellcome.platform.api.controllers

import com.google.inject.{Inject, Singleton}
import com.twitter.finagle.http.Request
import com.twitter.finatra.http.Controller
import uk.ac.wellcome.display.models.ApiVersions
import uk.ac.wellcome.platform.api.models.ApiConfig

@Singleton
class ContextController @Inject()(apiConfig: ApiConfig) extends Controller {

  prefix(apiConfig.pathPrefix) {
    setupContextEndpoint(ApiVersions.v1)
    setupContextEndpoint(ApiVersions.v2)
  }

  private def setupContextEndpoint(version: ApiVersions.Value): Unit = {
    get(s"/${version.toString}/context.json") { _: Request =>
      response.ok.file("context.json")
    }
  }
}
