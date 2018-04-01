package uk.ac.wellcome.platform.api.requests

import com.twitter.finagle.http.Request
import com.twitter.finatra.request.{QueryParam, RouteParam}
import com.twitter.finatra.validation.{Max, Min}
import uk.ac.wellcome.display.models.WorksIncludes

sealed trait ApiRequest {
  val request: Request
}

case class MultipleResultsRequest(
  @Min(1) @QueryParam page: Int = 1,
  @Min(1) @Max(100) @QueryParam pageSize: Option[Int],
  @QueryParam includes: Option[WorksIncludes],
  @RouteParam id: Option[String],
  @QueryParam query: Option[String],
  @QueryParam _index: Option[String],
  request: Request
) extends ApiRequest

case class SingleWorkRequest(
  @RouteParam id: String,
  @QueryParam includes: Option[WorksIncludes],
  @QueryParam _index: Option[String],
  request: Request
) extends ApiRequest
