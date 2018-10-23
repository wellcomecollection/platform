package uk.ac.wellcome.platform.api.controllers

import com.google.inject.{Inject, Singleton}
import com.twitter.finagle.http.Request
import com.twitter.finatra.http.Controller
import uk.ac.wellcome.platform.api.ContextHelper.buildContextUri
import uk.ac.wellcome.platform.api.models.{ApiConfig, DisplayError, Error}
import uk.ac.wellcome.platform.api.responses.ResultResponse

/** This controller returns a 404 to any requests for an undefined path.
  *
  * Note: it does this by defining a wildcard route for all paths.
  * Since Finatra resolves routes in the order they're declared, this
  * controller must be used last, or it will break other routes.
  *
  * More info:
  *   https://twitter.github.io/finatra/user-guide/http/controllers.html#wildcard-parameter
  *   https://alexwlchan.net/2018/10/finatra-404/
  *
  */
@Singleton
class MissingPathController @Inject()(apiConfig: ApiConfig) extends Controller {
  val contextUri: String = buildContextUri(apiConfig = apiConfig)

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
