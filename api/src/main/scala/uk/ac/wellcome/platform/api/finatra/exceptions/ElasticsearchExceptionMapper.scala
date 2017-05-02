package uk.ac.wellcome.platform.api.finatra.exceptions

import javax.inject.{Inject, Singleton}

import com.twitter.finagle.http.{Request, Response}
import com.twitter.finatra.http.exceptions.ExceptionMapper
import com.twitter.finatra.http.response.ResponseBuilder
import org.elasticsearch.ElasticsearchException

case class ErrorResponse(
  message: String
)

@Singleton
class ElasticsearchExceptionMapper @Inject()(response: ResponseBuilder)
    extends ExceptionMapper[ElasticsearchException] {

  override def toResponse(request: Request,
                          exception: ElasticsearchException): Response = {
    val errorResponse = ErrorResponse(exception.getMessage)

    response.internalServerError.json(errorResponse)
  }
}
