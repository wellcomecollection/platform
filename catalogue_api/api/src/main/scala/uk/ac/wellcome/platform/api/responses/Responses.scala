package uk.ac.wellcome.platform.api.responses

import com.fasterxml.jackson.annotation.{JsonProperty, JsonUnwrapped}
import uk.ac.wellcome.display.models.{DisplayWork, WorksIncludes}
import uk.ac.wellcome.platform.api.models.DisplayResultList
import uk.ac.wellcome.platform.api.requests.{ApiRequest, MultipleResultsRequest}

case class ResultResponse(
  @JsonProperty("@context") context: String,
  @JsonUnwrapped result: Any
)

case class ResultListResponse(
  @JsonProperty("@context") context: String,
  @JsonProperty("type") ontologyType: String,
  pageSize: Int = 10,
  totalPages: Int = 10,
  totalResults: Int = 100,
  results: List[_ <: Any],
  prevPage: Option[String] = None,
  nextPage: Option[String] = None
)

object ResultListResponse {
  def create[T <: DisplayWork,
             M <: MultipleResultsRequest[W],
             W <: WorksIncludes](
    contextUri: String,
    displayResultList: DisplayResultList[T],
    multipleResultsRequest: M,
    requestBaseUri: String
  ): ResultListResponse = {

    val currentPage = multipleResultsRequest.page
    val isLastPage = displayResultList.totalPages == currentPage
    val isOutOfBounds = displayResultList.totalPages < currentPage
    val isFirstPage = currentPage == 1

    val apiLink = createApiLink(requestBaseUri, multipleResultsRequest) _

    // We want to link to the previous *non-empty* page.
    // e.g. if there are 5 pages and you're looking at page 10, this should
    // link to page 5, not page 9.
    val prevLink =
      if (!isFirstPage) {
        val pageNumber = List(currentPage - 1, displayResultList.totalPages).min
        Some(apiLink(Map("page" -> pageNumber)))
      } else None

    val nextLink =
      if (!isLastPage && !isOutOfBounds)
        Some(apiLink(Map("page" -> (currentPage + 1))))
      else None

    ResultListResponse(
      context = contextUri,
      ontologyType = displayResultList.ontologyType,
      results = displayResultList.results,
      pageSize = displayResultList.pageSize,
      totalPages = displayResultList.totalPages,
      totalResults = displayResultList.totalResults,
      prevPage = prevLink,
      nextPage = nextLink
    )
  }

  private def createApiLink(
    requestBaseUri: String,
    apiRequest: ApiRequest
  )(
    updateMap: Map[String, Any]
  ): String = {

    val baseUrl = s"$requestBaseUri${apiRequest.request.path}"
    val queryString = (apiRequest.request.params ++ updateMap).toString()

    s"$baseUrl$queryString"
  }
}
