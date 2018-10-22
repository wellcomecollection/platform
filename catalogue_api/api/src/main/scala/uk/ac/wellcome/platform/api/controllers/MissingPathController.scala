package uk.ac.wellcome.platform.api.controllers

import com.google.inject.{Inject, Singleton}
import com.twitter.finagle.http.Request
import com.twitter.finatra.http.Controller
import uk.ac.wellcome.display.models.ApiVersions
import uk.ac.wellcome.platform.api.ContextHelper.buildContextUri
import uk.ac.wellcome.platform.api.models.{ApiConfig, DisplayError, Error}
import uk.ac.wellcome.platform.api.responses.ResultResponse

@Singleton
class MissingPathController @Inject()(apiConfig: ApiConfig) extends Controller {
  val contextUri: String = buildContextUri(
    apiConfig = apiConfig,
    version = ApiVersions.default
  )

  get("/:*") { request: Request =>
    val result = Error(
      variant = "http-404",
      description = Some(s"Page not found for URL ${request.uri}")
    )

    response.notFound.json(
      ResultResponse(context = contextUri, result = DisplayError(result))
    )
  }
}
