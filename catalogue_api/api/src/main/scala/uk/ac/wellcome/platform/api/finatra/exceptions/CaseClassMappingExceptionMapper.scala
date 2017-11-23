package uk.ac.wellcome.platform.api.finatra.exceptions

import javax.inject.{Inject, Singleton}

import com.twitter.finagle.http.{Request, Response}
import com.twitter.finatra.http.exceptions.ExceptionMapper
import com.twitter.finatra.http.response.ResponseBuilder
import com.twitter.finatra.json.internal.caseclass.exceptions.CaseClassMappingException
import com.twitter.inject.annotations.Flag
import com.twitter.inject.Logging

import uk.ac.wellcome.models.Error
import uk.ac.wellcome.platform.api.models.DisplayError
import uk.ac.wellcome.platform.api.responses.ResultResponse

@Singleton
class CaseClassMappingExceptionWrapper @Inject()(
  response: ResponseBuilder,
  @Flag("api.context") apiContext: String,
  @Flag("api.host") apiHost: String,
  @Flag("api.scheme") apiScheme: String)
    extends ExceptionMapper[CaseClassMappingException]
    with Logging {

  val contextUri: String = s"${apiScheme}://${apiHost}${apiContext}"

  override def toResponse(request: Request,
                          e: CaseClassMappingException): Response = {
    val errorString = e.errors
      .map { _.getMessage }
      .toList
      .mkString(", ")
    error(s"Sending HTTP 400 for $errorString")
    val result = DisplayError(
      Error(variant = "http-400", description = Some(errorString)))
    val errorResponse = ResultResponse(context = contextUri, result = result)

    response.badRequest.json(errorResponse)
  }
}
