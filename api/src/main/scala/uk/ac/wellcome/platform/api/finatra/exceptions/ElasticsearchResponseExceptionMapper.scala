package uk.ac.wellcome.platform.api.finatra.exceptions

import javax.inject.{Inject, Singleton}

import com.fasterxml.jackson.databind.{JsonNode, ObjectMapper}
import com.twitter.finagle.http.{Request, Response}
import com.twitter.finatra.http.exceptions.ExceptionMapper
import com.twitter.finatra.http.response.ResponseBuilder
import com.twitter.inject.annotations.Flag
import com.twitter.inject.Logging
import org.elasticsearch.client.ResponseException

import uk.ac.wellcome.models.Error
import uk.ac.wellcome.platform.api.models.DisplayError
import uk.ac.wellcome.platform.api.responses.ResultResponse

@Singleton
class ElasticsearchResponseExceptionMapper @Inject()(
  response: ResponseBuilder,
  @Flag("api.context") apiContext: String,
  @Flag("api.host") apiHost: String,
  @Flag("api.scheme") apiScheme: String)
    extends ExceptionMapper[ResponseException]
    with Logging {

  val contextUri: String = s"${apiScheme}://${apiHost}${apiContext}"

  private def userError(message: String, exception: Exception): DisplayError = {
    error(s"Sending HTTP 400 from ElasticsearchResponseExceptionMapper ($message)",
          exception)
    DisplayError(Error(variant = "http-400", description = Some(message)))
  }

  private def serverError(message: String, exception: Exception): DisplayError = {
    error(s"Sending HTTP 500 from ElasticsearchResponseExceptionMapper ($message)",
          exception)
    DisplayError(Error(variant = "http-500", description = None))
  }

  // One possible error is of the form:
  //
  //     Result window is too large, from + size must be less than or equal
  //     to: [10000] but was [100000]. See the scroll api for a more
  //     efficient way to request large data sets. This limit can be set by
  //     changing the [index.max_result_window] index level setting.
  //
  // In this case, we return a 400 Bad Request error, but we rephrase it
  // to be shorter and to avoid talking about Elasticsearch concepts.
  val resultSizePattern =
    """Result window is too large, from \+ size must be less than or equal to: \[([0-9]+)\]""".r.unanchored

  /* Elasticsearch errors have a "root_cause" with a reason -- given such
   * a reason, return an appropriate DisplayError.
   */
  private def reasonToError(reason: String): String =
    reason match {
      case resultSizePattern(size) => s"Only the first $size works are available in the API."
      case _ => "Unknown reason for error in Elasticsearch response: $reason"
    }

  private def toError(request: Request,
                      exception: ResponseException): DisplayError = {

    // Elasticsearch returns errors as JSON documents, which are stored in the
    // `message` attribute of ElasticsearchException.  Try to read it as JSON,
    // so we can check if this was a user error -- but if parsing fails, it's
    // enough to return a 500 error.

    try {
      // Annoyingly, the exact format of message is
      //
      //    POST http://localhost:9200/path: HTTP/1.1 500 Internal Server Error
      //    {"error":{...}}
      //
      // so we need to read the second line to get the actual JSON.
      //
      // Except when it isn't!  Sometimes these exceptions don't include
      // a JSON response, so there's only a single-line message -- then `.head`
      // fails: "NoSuchElementException: next on empty iterator".
      val mapper = new ObjectMapper()
      val jsonDocument = exception.getMessage
        .split("\n")
        .tail
        .head

      val exceptionData = mapper.readTree(jsonDocument)
      val reason = exceptionData
        .get("error")
        .get("root_cause")
        .get(0)
        .get("reason")
      reason match {
        case s: JsonNode => serverError(reasonToError(s.asText), exception)
        case _ =>
          serverError("Unable to find error reason in Elasticsearch response",
                      exception)
      }
    } catch {
      case e: Exception =>
        serverError(s"Error ($e) parsing Elasticsearch response", exception)
    }
  }

  override def toResponse(request: Request,
                          exception: ResponseException): Response = {
    val result = toError(request = request, exception = exception)
    val errorResponse = ResultResponse(context = contextUri, result = result)
    result.httpStatus.get match {
      case 500 => response.internalServerError.json(errorResponse)
      case 400 => response.badRequest.json(errorResponse)
    }
  }
}
