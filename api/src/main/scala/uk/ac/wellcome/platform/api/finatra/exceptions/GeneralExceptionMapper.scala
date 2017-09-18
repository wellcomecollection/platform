package uk.ac.wellcome.platform.api.finatra.exceptions

import javax.inject.{Inject, Singleton}

import com.twitter.finagle.http.{Request, Response}
import com.twitter.finatra.http.exceptions.ExceptionMapper
import com.twitter.finatra.http.response.ResponseBuilder
import com.twitter.inject.annotations.Flag
import com.twitter.inject.Logging

import uk.ac.wellcome.models.Error
import uk.ac.wellcome.platform.api.models.DisplayError
import uk.ac.wellcome.platform.api.responses.ResultResponse

@Singleton
class GeneralExceptionMapper @Inject()(response: ResponseBuilder,
                                       @Flag("api.context") apiContext: String,
                                       @Flag("api.host") apiHost: String,
                                       @Flag("api.scheme") apiScheme: String)
    extends ExceptionMapper[Exception]
    with Logging {

  val contextUri: String = s"${apiScheme}://${apiHost}${apiContext}"

  override def toResponse(request: Request, exception: Exception): Response = {
    error(s"Sending HTTP 500 from GeneralExceptionMapper", exception)
    val result = DisplayError(
      Error(
        variant = "http-500",
        description = None
      ))
    val errorResponse = ResultResponse(context = contextUri, result = result)
    response.internalServerError.json(errorResponse)
  }
}
