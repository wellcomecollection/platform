package uk.ac.wellcome.platform.api.works

import com.sksamuel.elastic4s.Index
import com.twitter.finagle.http.{Response, Status}
import com.twitter.finatra.http.EmbeddedHttpServer
import uk.ac.wellcome.test.fixtures.TestWith

trait ApiErrorsTestBase { this: ApiWorksTestBase =>
  def withApi[R]: TestWith[(String, Index, Index, EmbeddedHttpServer), R] => R

  describe("returns a 400 Bad Request for user errors") {
    it("when it gets malformed query parameters") {
      assertIsBadRequest(
        "/works?pageSize=penguin",
        description = "pageSize: 'penguin' is not a valid Integer"
      )
    }

    // We want to cover multiple cases with the page size tests:
    //
    //    - Just beyond the upper limit
    //    - Just below the lower limit
    //    - Large, but low enoguh that Elasticsearch is still able to tell us
    //      what went wrong.
    //

    it("when asked for a page size just over the maximum") {
      val pageSize = 101
      assertIsBadRequest(
        s"/works?pageSize=$pageSize",
        description = s"pageSize: [$pageSize] is not less than or equal to 100"
      )
    }

    it("when asked for a zero-length page") {
      val pageSize = 0
      assertIsBadRequest(
        s"/works?pageSize=$pageSize",
        description = s"pageSize: [$pageSize] is not greater than or equal to 1"
      )
    }

    it("when asked for a large page size") {
      val pageSize = 100000
      assertIsBadRequest(
        s"/works?pageSize=$pageSize",
        description = s"pageSize: [$pageSize] is not less than or equal to 100"
      )
    }
  }

  describe("returns a 404 Not Found for missing resources") {
    it("when asked for a work that doesn't exist") {
      val badId = "doesnotexist"
      assertIsNotFound(
        s"/works/$badId",
        description = s"Work not found for identifier $badId"
      )
    }
  }

  private def assertIsBadRequest(path: String, description: String): Response =
    withApi { case (apiPrefix, _, _, server: EmbeddedHttpServer) =>
      server.httpGet(
        path = s"/$apiPrefix$path",
        andExpect = Status.BadRequest,
        withJsonBody = badRequest(
          apiPrefix = apiPrefix,
          description = description
        )
      )
    }

  private def assertIsNotFound(path: String, description: String): Response =
    withApi { case (apiPrefix, _, _, server: EmbeddedHttpServer) =>
      server.httpGet(
        path = s"/$apiPrefix$path",
        andExpect = Status.NotFound,
        withJsonBody = notFound(
          apiPrefix = apiPrefix,
          description = description
        )
      )
    }
}
