package uk.ac.wellcome.platform.api.requests

import com.twitter.finagle.http.Request
import com.twitter.finatra.request.{QueryParam, RouteParam}
import com.twitter.finatra.validation.{Max, Min}
import uk.ac.wellcome.display.models.{V1WorksIncludes, V2WorksIncludes, WorksIncludes}

sealed trait ApiRequest {
  val request: Request
}

trait MultipleResultsRequest extends ApiRequest {
  val page: Int
  val pageSize: Option[Int]
  val include: Option[WorksIncludes]
  val query: Option[String]
  val _index: Option[String]
  val request: Request
}

case class V1MultipleResultsRequest(
  @Min(1) @QueryParam page: Int = 1,
  @Min(1) @Max(100) @QueryParam pageSize: Option[Int],
  @QueryParam includes: Option[V1WorksIncludes],
  @QueryParam query: Option[String],
  @QueryParam _index: Option[String],
  request: Request
) extends MultipleResultsRequest {
  val include: Option[WorksIncludes] = includes
}

case class V2MultipleResultsRequest(
  @Min(1) @QueryParam page: Int = 1,
  @Min(1) @Max(100) @QueryParam pageSize: Option[Int],
  @QueryParam include: Option[V2WorksIncludes],
  @QueryParam query: Option[String],
  @QueryParam _index: Option[String],
  request: Request
) extends MultipleResultsRequest

trait SingleWorkRequest {
  val id: String
  val include: Option[WorksIncludes]
  val _index: Option[String]
  val request: Request
}

case class V1SingleWorkRequest(
  @RouteParam id: String,
  @QueryParam includes: Option[V1WorksIncludes],
  @QueryParam _index: Option[String],
  request: Request
) extends SingleWorkRequest {
  val include = includes
}

case class V2SingleWorkRequest(
  @RouteParam id: String,
  @QueryParam include: Option[V2WorksIncludes],
  @QueryParam _index: Option[String],
  request: Request
) extends SingleWorkRequest
