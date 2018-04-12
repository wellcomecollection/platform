package uk.ac.wellcome.platform.api.finatra.exceptions

import com.twitter.finagle.http.{Request, Response}
import com.twitter.finatra.http.exceptions.ExceptionMapper
import com.twitter.finatra.http.response.ResponseBuilder
import com.twitter.finatra.json.internal.caseclass.exceptions.CaseClassMappingException
import com.twitter.inject.Logging
import com.twitter.inject.annotations.Flag
import javax.inject.{Inject, Singleton}
import uk.ac.wellcome.models.Error
import uk.ac.wellcome.platform.api.ContextHelper.buildContextUri
import uk.ac.wellcome.platform.api.models.DisplayError
import uk.ac.wellcome.platform.api.responses.ResultResponse

@Singleton
class CaseClassMappingExceptionWrapper @Inject()(
  response: ResponseBuilder,
  @Flag("api.context.suffix") apiContextSuffix: String,
  @Flag("api.host") apiHost: String,
  @Flag("api.prefix") apiPrefix: String,
  @Flag("api.scheme") apiScheme: String)
    extends ExceptionMapper[CaseClassMappingException]
    with Logging {

  override def toResponse(request: Request,
                          e: CaseClassMappingException): Response = {

    val version = getVersion(request, s"$apiPrefix")

    val errorString = e.errors
      .map { _.getMessage }
      .toList
      .mkString(", ")
    error(s"Sending HTTP 400 for $errorString")
    val result = DisplayError(
      Error(variant = "http-400", description = Some(errorString)))
    val errorResponse = ResultResponse(
      context = buildContextUri(
        apiScheme,
        apiHost,
        apiPrefix,
        version,
        apiContextSuffix),
      result = result)

    response.badRequest.json(errorResponse)
  }
}
