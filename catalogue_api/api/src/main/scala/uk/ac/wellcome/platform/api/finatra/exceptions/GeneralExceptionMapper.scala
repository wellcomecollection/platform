package uk.ac.wellcome.platform.api.finatra.exceptions

import com.google.inject.{Inject, Singleton}
import com.twitter.finagle.http.{Request, Response}
import com.twitter.finatra.http.exceptions.ExceptionMapper
import com.twitter.finatra.http.response.ResponseBuilder
import com.twitter.inject.Logging
import uk.ac.wellcome.platform.api.ContextHelper.buildContextUri
import uk.ac.wellcome.platform.api.models.{ApiConfig, DisplayError, Error}
import uk.ac.wellcome.platform.api.responses.ResultResponse

@Singleton
class GeneralExceptionMapper @Inject()(response: ResponseBuilder,
                                       apiConfig: ApiConfig)
    extends ExceptionMapper[Exception]
    with Logging {

  override def toResponse(request: Request, exception: Exception): Response = {

    val version = getVersion(request, apiPrefix = apiConfig.pathPrefix)

    error(s"Sending HTTP 500 from GeneralExceptionMapper", exception)
    val result = DisplayError(
      Error(
        variant = "http-500",
        description = None
      ))
    val errorResponse = ResultResponse(
      context = buildContextUri(apiConfig = apiConfig, version = version),
      result = result)
    response.internalServerError.json(errorResponse)
  }
}
