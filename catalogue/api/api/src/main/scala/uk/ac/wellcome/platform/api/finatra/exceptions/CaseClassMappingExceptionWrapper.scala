package uk.ac.wellcome.platform.api.finatra.exceptions

import com.google.inject.{Inject, Singleton}
import com.twitter.finagle.http.{Request, Response}
import com.twitter.finatra.http.exceptions.ExceptionMapper
import com.twitter.finatra.http.response.ResponseBuilder
import com.twitter.finatra.json.internal.caseclass.exceptions.CaseClassMappingException
import com.twitter.inject.Logging

import uk.ac.wellcome.platform.api.ContextHelper.buildContextUri
import uk.ac.wellcome.platform.api.models.{ApiConfig, DisplayError, Error}
import uk.ac.wellcome.platform.api.responses.ResultResponse

@Singleton
class CaseClassMappingExceptionWrapper @Inject()(response: ResponseBuilder,
                                                 apiConfig: ApiConfig)
    extends ExceptionMapper[CaseClassMappingException]
    with Logging {

  override def toResponse(request: Request,
                          e: CaseClassMappingException): Response = {

    val version = getVersion(request, apiPrefix = apiConfig.pathPrefix)

    val errorString = e.errors
      .map { _.getMessage }
      .toList
      .mkString(", ")
    error(s"Sending HTTP 400 for $errorString")
    val result = DisplayError(
      Error(variant = "http-400", description = Some(errorString)))
    val errorResponse = ResultResponse(
      context = buildContextUri(apiConfig = apiConfig, version = version),
      result = result
    )

    response.badRequest.json(errorResponse)
  }
}
