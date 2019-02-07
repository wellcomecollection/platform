package uk.ac.wellcome.platform.api.works

import com.twitter.finagle.http.{Response, Status}
import com.twitter.finatra.http.EmbeddedHttpServer
import uk.ac.wellcome.fixtures.TestWith

trait ApiErrorsTestBase { this: ApiWorksTestBase =>
  val apiPrefix: String

  def withServer[R](testWith: TestWith[EmbeddedHttpServer, R]): R

  describe("returns a 400 Bad Request for user errors") {
    describe("errors in the ?pageSize query") {
      it("not an integer") {
        val pageSize = "penguin"
        assertIsBadRequest(
          s"/works?pageSize=$pageSize",
          description = s"pageSize: '$pageSize' is not a valid Integer"
        )
      }

      it("just over the maximum") {
        val pageSize = 101
        assertIsBadRequest(
          s"/works?pageSize=$pageSize",
          description =
            s"pageSize: [$pageSize] is not less than or equal to 100"
        )
      }

      it("just below the minimum (zero)") {
        val pageSize = 0
        assertIsBadRequest(
          s"/works?pageSize=$pageSize",
          description =
            s"pageSize: [$pageSize] is not greater than or equal to 1"
        )
      }

      it("a large page size") {
        val pageSize = 100000
        assertIsBadRequest(
          s"/works?pageSize=$pageSize",
          description =
            s"pageSize: [$pageSize] is not less than or equal to 100"
        )
      }

      it("a negative page size") {
        val pageSize = -50
        assertIsBadRequest(
          s"/works?pageSize=$pageSize",
          description =
            s"pageSize: [$pageSize] is not greater than or equal to 1"
        )
      }
    }

    describe("errors in the ?page query") {
      it("page 0") {
        val page = 0
        assertIsBadRequest(
          s"/works?page=$page",
          description = s"page: [$page] is not greater than or equal to 1"
        )
      }

      it("a negative page") {
        val page = -50
        assertIsBadRequest(
          s"/works?page=$page",
          description = s"page: [$page] is not greater than or equal to 1"
        )
      }
    }

    describe("trying to get more works than ES allows") {
      val description = "Only the first 10000 works are available in the API. " +
        "If you want more works, you can download a snapshot of the complete catalogue: " +
        "https://developers.wellcomecollection.org/datasets"

      it("a very large page") {
        assertIsBadRequest(
          s"/works?page=10000",
          description = description
        )
      }

      // https://github.com/wellcometrust/platform/issues/3233
      it("so many pages that a naive (page * pageSize) would overflow") {
        assertIsBadRequest(
          s"/works?page=2000000000&pageSize=100",
          description = description
        )
      }

      it("the 101th page with 100 results per page") {
        assertIsBadRequest(
          s"/works?page=101&pageSize=100",
          description = description
        )
      }
    }

    it("returns multiple errors if there's more than one invalid parameter") {
      val pageSize = -60
      val page = -50
      assertIsBadRequest(
        s"/works?pageSize=$pageSize&page=$page",
        description =
          s"page: [$page] is not greater than or equal to 1, pageSize: [$pageSize] is not greater than or equal to 1"
      )
    }
  }

  describe("returns a 404 Not Found for missing resources") {
    it("looking up a work that doesn't exist") {
      val badId = "doesnotexist"
      assertIsNotFound(
        s"/works/$badId",
        description = s"Work not found for identifier $badId"
      )
    }

    it("looking up a work with a malformed identifier") {
      val badId = "zd224ncv]"
      assertIsNotFound(
        s"/works/$badId",
        description = s"Work not found for identifier $badId"
      )
    }

    describe("an index that doesn't exist") {
      val indexName = "foobarbaz"

      it("listing") {
        assertIsNotFound(
          s"/works?_index=$indexName",
          description = s"There is no index $indexName"
        )
      }

      it("looking up a work") {
        assertIsNotFound(
          s"/works/1234?_index=$indexName",
          description = s"There is no index $indexName"
        )
      }

      it("searching") {
        assertIsNotFound(
          s"/works/1234?_index=$indexName&query=foobar",
          description = s"There is no index $indexName"
        )
      }
    }
  }

  it("returns an Internal Server error if you try to search a malformed index") {
    // We need to do something that reliably triggers an internal exception
    // in the Elasticsearch handler.
    //
    // By creating an index without a mapping, we don't have a canonicalId field
    // to sort on.  Trying to query this index of these will trigger one such exception!
    withHttpServer { server: EmbeddedHttpServer =>
      withEmptyIndex { index =>
        server.httpGet(
          path = s"/${getApiPrefix()}/works?_index=${index.name}",
          andExpect = Status.InternalServerError,
          withJsonBody = s"""
               |{
               |  "@context": "https://localhost:8888/${getApiPrefix()}/context.json",
               |  "type": "Error",
               |  "errorType": "http",
               |  "httpStatus": 500,
               |  "label": "Internal Server Error"
               |}
               """.stripMargin
        )
      }
    }
  }

  def assertIsBadRequest(path: String, description: String): Response =
    withServer { server: EmbeddedHttpServer =>
      server.httpGet(
        path = s"/$apiPrefix$path",
        andExpect = Status.BadRequest,
        withJsonBody = badRequest(
          apiPrefix = apiPrefix,
          description = description
        )
      )
    }

  def assertIsNotFound(path: String, description: String): Response =
    withServer { server: EmbeddedHttpServer =>
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
