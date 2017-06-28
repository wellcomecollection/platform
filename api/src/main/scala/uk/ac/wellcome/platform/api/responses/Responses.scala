package uk.ac.wellcome.platform.api.responses

import com.fasterxml.jackson.annotation.{JsonProperty, JsonUnwrapped}
import com.twitter.finagle.http.ParamMap
import uk.ac.wellcome.platform.api.requests.{ApiRequest, MultipleResultsRequest}
import uk.ac.wellcome.platform.api.models.DisplaySearch

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
  prev: Option[String] = None,
  next: Option[String] = None
) {
  @JsonProperty("type") val ontologyType: String = "ResultList"
}

object ResultListResponse {

  private def getParamMapFromCustomCCRequestParams(cc: ApiRequest): ParamMap =
    ParamMap((Map[String, String]() /: cc.getClass.getDeclaredFields) {(a, f) =>
      f.setAccessible(true)

      if(f.getName != "request") {
        f.get(cc) match {
          case None => a
          case Some(o) => a + (f.getName -> o.toString)
          case o => a + (f.getName -> o.toString)
        }
      } else {
        a
      }
    })

  private def apiLink(
                       requestBaseUri: String,
                       apiRequest: ApiRequest
                     ): String = {

    val customRequestParamMap = getParamMapFromCustomCCRequestParams(apiRequest)
    s"${requestBaseUri}${apiRequest.request.path}${customRequestParamMap.toString()}"
  }

  def create(
              contextUri: String,
              displaySearch: DisplaySearch,
              multipleResultsRequest: MultipleResultsRequest,
              requestBaseUri: String
            ): ResultListResponse = {

    val isLastPage = displaySearch.totalPages == multipleResultsRequest.page
    val isFirstPage = multipleResultsRequest.page == 1
    val isOnlyPage = displaySearch.totalPages <= 1

    val prevPageRequest = multipleResultsRequest.copy(page = multipleResultsRequest.page - 1)
    val nextPageRequest = multipleResultsRequest.copy(page = multipleResultsRequest.page + 1)

    val prevLink = if(!isFirstPage && !isOnlyPage) Some(apiLink(requestBaseUri, prevPageRequest)) else None
    val nextLink = if(!isLastPage && !isOnlyPage) Some(apiLink(requestBaseUri, nextPageRequest)) else None

    ResultListResponse(
      context = contextUri,
      results = displaySearch.results,
      pageSize = displaySearch.pageSize,
      totalPages = displaySearch.totalPages,
      totalResults = displaySearch.totalResults,
      prev = prevLink,
      next = nextLink
    )
  }
}
