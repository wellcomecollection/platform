package uk.ac.wellcome.platform.api.responses

import com.twitter.finagle.http.{Method, Request}
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.display.models.V1WorksIncludes
import uk.ac.wellcome.display.models.v1.DisplayWorkV1
import uk.ac.wellcome.platform.api.models.DisplayResultList
import uk.ac.wellcome.platform.api.requests.V1MultipleResultsRequest

class ResultListResponseTest extends FunSpec with Matchers {
  val contextUri = "https://example.org/context.json"

  val requestBaseUri = "https://api.example.org"
  val requestUri = "/works"

  val displayResultList = DisplayResultList[DisplayWorkV1](
    pageSize = 10,
    totalPages = 5,
    totalResults = 45,
    // Nothing checks that results is populated correctly!
    results = List()
  )

  val multipleResultsRequest = V1MultipleResultsRequest(
    page = 1,
    pageSize = Some(displayResultList.pageSize),
    includes = None,
    query = None,
    _index = None,
    request = Request(method = Method.Get, uri = requestUri)
  )

  it("inclues a nextPage and prevPage parameter where appropriate") {
    val resp = getResponse(
      displayResultList = displayResultList.copy(totalPages = 5),
      multipleResultsRequest = multipleResultsRequest.copy(page = 3)
    )

    resp.prevPage shouldBe Some(s"$requestBaseUri$requestUri?page=2")
    resp.nextPage shouldBe Some(s"$requestBaseUri$requestUri?page=4")
  }

  describe("nextPage") {
    it("omits the parameter if this is the only page") {
      val resp = getResponse(
        displayResultList = displayResultList.copy(totalPages = 1)
      )

      resp.nextPage shouldBe None
    }

    it("omits the parameter if this is the last page") {
      val resp = getResponse(
        displayResultList = displayResultList.copy(totalPages = 5),
        multipleResultsRequest = multipleResultsRequest.copy(page = 5)
      )

      resp.nextPage shouldBe None
    }

    it("omits the parameter if the request is beyond the last page") {
      val resp = getResponse(
        displayResultList = displayResultList.copy(totalPages = 5),
        multipleResultsRequest = multipleResultsRequest.copy(page = 10)
      )

      resp.nextPage shouldBe None
    }

    it("includes the parameter if there's a next page to view") {
      val resp = getResponse(
        displayResultList = displayResultList.copy(totalPages = 10),
        multipleResultsRequest = multipleResultsRequest.copy(page = 5)
      )

      resp.nextPage shouldBe Some(s"$requestBaseUri$requestUri?page=6")
    }
  }

  describe("prevPage") {
    it("omits the parameter if this is the only page") {
      val resp = getResponse(
        displayResultList = displayResultList.copy(totalPages = 1)
      )

      resp.prevPage shouldBe None
    }

    it("omits the parameter if this is the first page") {
      val resp = getResponse(
        displayResultList = displayResultList.copy(totalPages = 5),
        multipleResultsRequest = multipleResultsRequest.copy(page = 1)
      )

      resp.prevPage shouldBe None
    }

    it("includes the parameter if there's a previous page to view") {
      val resp = getResponse(
        displayResultList = displayResultList.copy(totalPages = 10),
        multipleResultsRequest = multipleResultsRequest.copy(page = 5)
      )

      resp.prevPage shouldBe Some(s"$requestBaseUri$requestUri?page=4")
    }

    it("links to the last populated page if beyond the end") {
      val resp = getResponse(
        displayResultList = displayResultList.copy(totalPages = 5),
        multipleResultsRequest = multipleResultsRequest.copy(page = 10)
      )

      resp.prevPage shouldBe Some(s"$requestBaseUri$requestUri?page=5")
    }
  }

  private def getResponse(
    contextUri: String = contextUri,
    displayResultList: DisplayResultList[DisplayWorkV1],
    multipleResultsRequest: V1MultipleResultsRequest = multipleResultsRequest,
    requestBaseUri: String = requestBaseUri
  ): ResultListResponse =
    ResultListResponse
      .create[DisplayWorkV1, V1MultipleResultsRequest, V1WorksIncludes](
        contextUri = contextUri,
        displayResultList = displayResultList,
        multipleResultsRequest = multipleResultsRequest,
        requestBaseUri = requestBaseUri
      )
}
