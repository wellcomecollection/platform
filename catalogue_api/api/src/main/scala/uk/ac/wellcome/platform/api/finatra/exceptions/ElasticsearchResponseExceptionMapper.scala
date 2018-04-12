package uk.ac.wellcome.platform.api.finatra.exceptions

import com.fasterxml.jackson.databind.ObjectMapper
import com.twitter.finagle.http.{Request, Response}
import com.twitter.finatra.http.exceptions.ExceptionMapper
import com.twitter.finatra.http.response.ResponseBuilder
import com.twitter.inject.Logging
import com.twitter.inject.annotations.Flag
import javax.inject.{Inject, Singleton}
import org.elasticsearch.client.ResponseException
import uk.ac.wellcome.models.Error
import uk.ac.wellcome.platform.api.ContextHelper.buildContextUri
import uk.ac.wellcome.platform.api.models.DisplayError
import uk.ac.wellcome.platform.api.responses.ResultResponse

@Singleton
class ElasticsearchResponseExceptionMapper @Inject()(
  response: ResponseBuilder,
  @Flag("api.context.suffix") apiContextSuffix: String,
  @Flag("api.host") apiHost: String,
  @Flag("api.prefix") apiPrefix: String,
  @Flag("api.scheme") apiScheme: String)
    extends ExceptionMapper[ResponseException]
    with Logging {

  // This error is of the form:
  //
  //     Result window is too large, from + size must be less than or equal
  //     to: [10000] but was [100000]. See the scroll api for a more
  //     efficient way to request large data sets. This limit can be set by
  //     changing the [index.max_result_window] index level setting.
  //
  // When returning a 400 to the user, we wrap this error to avoid talking
  // about internal Elasticsearch concepts.
  private val resultSizePattern =
    """Result window is too large, from \+ size must be less than or equal to: \[([0-9]+)\]""".r.unanchored

  override def toResponse(request: Request,
                          exception: ResponseException): Response = {
    val result = toError(exception = exception)
    val version = getVersion(request, s"$apiPrefix")

    val errorResponse = ResultResponse(
      context = buildContextUri(
        apiScheme,
        apiHost,
        apiPrefix,
        version,
        apiContextSuffix),
      result = result)
    result.httpStatus.get match {
      case 500 => response.internalServerError.json(errorResponse)
      case 404 => response.notFound.json(errorResponse)
      case 400 => response.badRequest.json(errorResponse)
    }
  }

  private def toError(exception: ResponseException): DisplayError = {
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
      // fails: "NoSuchElementException: next on empty iterator".  I think
      // this happens when the cluster doesn't reply, but I'm not sure.
      val jsonDocument = exception.getMessage
        .split("\n")
        .tail
        .head
      jsonToError(jsonDocument = jsonDocument, exception = exception)
    } catch {
      case e: Exception =>
        serverError(s"Error ($e) parsing Elasticsearch response", exception)
    }
  }

  private def jsonToError(jsonDocument: String,
                          exception: Exception): DisplayError = {
    val mapper = new ObjectMapper()
    val exceptionData = mapper.readTree(jsonDocument)

    val esError = exceptionData.get("error")
    val esErrorType = esError
      .get("type")
      .asText

    esErrorType match {
      // This occurs if the user requests a non-existent index as ?_index=foo.
      // We return this as a 404 error to the user.
      case "index_not_found_exception" =>
        val index = esError
          .get("index")
          .asText
        notFound(s"There is no index $index", exception)

      // This may occur if the user requests an overly large page of results.
      // We return this as a 400 error to the user.
      case "search_phase_execution_exception" =>
        val reason = esError
          .get("root_cause")
          .get(0)
          .get("reason")
          .asText

        resultSizePattern.findFirstMatchIn(reason) match {
          case Some(s) =>
            val size = s.group(1)
            userError(
              s"Only the first $size works are available in the API.",
              exception)
          case _ =>
            serverError(
              s"Unknown error in search phase execution: $reason",
              exception)
        }

      // Anything else should bubble up as a 500, as it's at least somewhat
      // unexpected and worthy of further investigation.
      case _ =>
        serverError("Unknown error", exception)
    }
  }

  private def userError(message: String, exception: Exception): DisplayError = {
    error(
      s"Sending HTTP 400 from ElasticsearchResponseExceptionMapper ($message)",
      exception)
    DisplayError(Error(variant = "http-400", description = Some(message)))
  }

  private def notFound(message: String, exception: Exception): DisplayError = {
    error(
      s"Sending HTTP 404 from ElasticsearchResponseExceptionMapper ($message)",
      exception)
    DisplayError(Error(variant = "http-404", description = Some(message)))
  }

  private def serverError(message: String,
                          exception: Exception): DisplayError = {
    error(
      s"Sending HTTP 500 from ElasticsearchResponseExceptionMapper ($message)",
      exception)
    DisplayError(Error(variant = "http-500", description = None))
  }
}
