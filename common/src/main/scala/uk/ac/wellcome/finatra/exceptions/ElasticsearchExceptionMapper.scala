package uk.ac.wellcome.finatra.exceptions

import org.elasticsearch.ElasticsearchException
import com.twitter.finagle.http.{Request, Response}
import com.twitter.finatra.http.response.ResponseBuilder
import com.twitter.finatra.http.exceptions.ExceptionMapper
import javax.inject.{Inject, Singleton}

case class ErrorResponse(
  message: String
)

@Singleton
class ElasticsearchExceptionMapper @Inject()(response: ResponseBuilder)
  extends ExceptionMapper[ElasticsearchException] {

  override def toResponse(request: Request, exception: ElasticsearchException): Response = {
    val errorResponse = ErrorResponse(exception.getMessage)

    response.internalServerError.json(errorResponse)
  }
}
