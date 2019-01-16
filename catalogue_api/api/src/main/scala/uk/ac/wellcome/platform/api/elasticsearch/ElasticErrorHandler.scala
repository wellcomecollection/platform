package uk.ac.wellcome.platform.api.elasticsearch

import com.sksamuel.elastic4s.http.ElasticError
import grizzled.slf4j.Logging
import uk.ac.wellcome.platform.api.models.{DisplayError, Error}

object ElasticErrorHandler extends Logging {

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

  def buildDisplayError(elasticError: ElasticError): DisplayError =
    elasticError.`type` match {
      // This occurs if the user requests a non-existent index as ?_index=foo.
      // We return this as a 404 error to the user.
      case "index_not_found_exception" =>
        val index = elasticError.index.get
        notFound(s"There is no index $index", elasticError)

      // This may occur if the user requests an overly large page of results.
      // We return this as a 400 error to the user.
      case "search_phase_execution_exception" =>
        val reason = elasticError.rootCause.mkString("; ")

        resultSizePattern.findFirstMatchIn(reason) match {
          case Some(s) =>
            val size = s.group(1)
            userError(
              s"Only the first $size works are available in the API. " +
                "If you want more works, you can download a snapshot of the complete catalogue: " +
                "https://developers.wellcomecollection.org/datasets",
              elasticError
            )
          case _ =>
            serverError(
              s"Unknown error in search phase execution: $reason",
              elasticError)
        }

      // Anything else should bubble up as a 500, as it's at least somewhat
      // unexpected and worthy of further investigation.
      case _ =>
        serverError("Unknown error", elasticError)
    }

  private def userError(message: String,
                        elasticError: ElasticError): DisplayError = {
    warn(
      s"Sending HTTP 400 from ${this.getClass.getSimpleName} ($message; $elasticError)")
    DisplayError(Error(variant = "http-400", description = Some(message)))
  }

  private def notFound(message: String,
                       elasticError: ElasticError): DisplayError = {
    warn(
      s"Sending HTTP 404 from ${this.getClass.getSimpleName} ($message; $elasticError)")
    DisplayError(Error(variant = "http-404", description = Some(message)))
  }

  private def serverError(message: String,
                          elasticError: ElasticError): DisplayError = {
    error(
      s"Sending HTTP 500 from ${this.getClass.getSimpleName} ($message; $elasticError)")
    DisplayError(Error(variant = "http-500", description = None))
  }
}
