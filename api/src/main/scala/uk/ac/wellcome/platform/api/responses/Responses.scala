package uk.ac.wellcome.platform.api.responses

import com.fasterxml.jackson.annotation.{JsonProperty, JsonUnwrapped}
import uk.ac.wellcome.platform.api.models.DisplaySearch
import uk.ac.wellcome.platform.api.requests.{
  ApiRequest,
  MultipleResultsRequest
}

import scala.language.existentials

case class ResultResponse(
  @JsonProperty("@context") context: String,
  @JsonUnwrapped result: Any
)

case class ResultListResponse(
  @JsonProperty("@context") context: String,
  pageSize: Int = 10,
  totalPages: Int = 10,
  totalResults: Int = 100,
  results: Array[_ <: Any],
  prevPage: Option[String] = None,
  nextPage: Option[String] = None
) {
  @JsonProperty("type") val ontologyType: String = "ResultList"
}

object ResultListResponse {
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

  def create(
    contextUri: String,
    displaySearch: DisplaySearch,
    multipleResultsRequest: MultipleResultsRequest,
    requestBaseUri: String
  ): ResultListResponse = {

    val currentPage = multipleResultsRequest.page
    val isLastPage = displaySearch.totalPages == currentPage
    val isFirstPage = currentPage == 1
    val isOnlyPage = displaySearch.totalPages <= 1

    val apiLink = createApiLink(requestBaseUri, multipleResultsRequest) _

    val prevLink =
      if (!isFirstPage && !isOnlyPage)
        Some(apiLink(Map("page" -> (currentPage - 1))))
      else None
    val nextLink =
      if (!isLastPage && !isOnlyPage)
        Some(apiLink(Map("page" -> (currentPage + 1))))
      else None

    ResultListResponse(
      context = contextUri,
      results = displaySearch.results,
      pageSize = displaySearch.pageSize,
      totalPages = displaySearch.totalPages,
      totalResults = displaySearch.totalResults,
      prevPage = prevLink,
      nextPage = nextLink
    )
  }
}
