package uk.ac.wellcome.platform.api.finatra.exceptions

import javax.inject.{Inject, Singleton}
import com.twitter.finagle.http.{Request, Response}
import com.twitter.finatra.http.exceptions.ExceptionMapper
import com.twitter.finatra.http.response.ResponseBuilder
import com.twitter.inject.annotations.Flag
import com.twitter.inject.Logging
import uk.ac.wellcome.models.Error
import uk.ac.wellcome.platform.api.ContextHelper.buildContextUri
import uk.ac.wellcome.platform.api.models.DisplayError
import uk.ac.wellcome.platform.api.responses.ResultResponse

@Singleton
class GeneralExceptionMapper @Inject()(
  response: ResponseBuilder,
  @Flag("api.context.suffix") apiContextSuffix: String,
  @Flag("api.host") apiHost: String,
  @Flag("api.prefix") apiPrefix: String,
  @Flag("api.scheme") apiScheme: String)
    extends ExceptionMapper[Exception]
    with Logging {

  override def toResponse(request: Request, exception: Exception): Response = {

    val version = getVersion(request, s"$apiPrefix")

    error(s"Sending HTTP 500 from GeneralExceptionMapper", exception)
    val result = DisplayError(
      Error(
        variant = "http-500",
        description = None
      ))
    val errorResponse = ResultResponse(
      context = buildContextUri(
        apiScheme,
        apiHost,
        apiPrefix,
        version,
        apiContextSuffix),
      result = result)
    response.internalServerError.json(errorResponse)
  }
}
