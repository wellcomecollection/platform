package uk.ac.wellcome.platform.api.works

import com.twitter.finagle.http.Status
import com.twitter.finatra.http.EmbeddedHttpServer
import org.scalatest.prop.PropertyChecks
import uk.ac.wellcome.models.ApiVersions
import org.scalacheck.ScalacheckShapeless._

class ApiErrorsTest extends ApiWorksTestBase with PropertyChecks {

  it(
    "returns a BadRequest error when malformed query parameters are presented") {
    forAll { version: ApiVersions.Value =>
      withApiFixtures(apiVersion = version) { case (apiPrefix, _, _, server: EmbeddedHttpServer) =>
        server.httpGet(
          path = s"/$apiPrefix/works?pageSize=penguin",
          andExpect = Status.BadRequest,
          withJsonBody = badRequest(apiPrefix, "pageSize: 'penguin' is not a valid Integer")
        )
      }
    }
  }

  it("returns a NotFound error when requesting a work with a non-existent id") {
    forAll { version: ApiVersions.Value =>
      withApiFixtures(apiVersion = version) { case (apiPrefix, _, _, server: EmbeddedHttpServer) =>
        val badId = "non-existing-id"
        server.httpGet(
          path = s"/$apiPrefix/works/$badId",
          andExpect = Status.NotFound,
          withJsonBody = notFound(apiPrefix, s"Work not found for identifier $badId")
        )
      }
    }
  }

  it(
    "returns a BadRequest error if the user asks for a page size just over the maximum") {
    forAll { version: ApiVersions.Value =>
      withApiFixtures(apiVersion = version) { case (apiPrefix, _, _, server: EmbeddedHttpServer) =>
        val pageSize = 101
        server.httpGet(
          path = s"/$apiPrefix/works?pageSize=$pageSize",
          andExpect = Status.BadRequest,
          withJsonBody =
            badRequest(apiPrefix, s"pageSize: [$pageSize] is not less than or equal to 100")
        )
      }
    }
  }

  it(
    "returns a BadRequest error if the user asks for an overly large page size") {
    forAll { version: ApiVersions.Value =>
      withApiFixtures(apiVersion = version) { case (apiPrefix, _, _, server: EmbeddedHttpServer) =>
        val pageSize = 100000
        server.httpGet(
          path = s"/$apiPrefix/works?pageSize=$pageSize",
          andExpect = Status.BadRequest,
          withJsonBody =
            badRequest(apiPrefix, s"pageSize: [$pageSize] is not less than or equal to 100")
        )
      }
    }
  }

  it("returns a BadRequest error if the user asks for zero-length pages") {
    forAll { version: ApiVersions.Value =>
      withApiFixtures(apiVersion = version) { case (apiPrefix, _, _, server: EmbeddedHttpServer) =>
        val pageSize = 0
        server.httpGet(
          path = s"/$apiPrefix/works?pageSize=$pageSize",
          andExpect = Status.BadRequest,
          withJsonBody =
            badRequest(apiPrefix, s"pageSize: [$pageSize] is not greater than or equal to 1")
        )
      }
    }
  }

  it("returns a BadRequest error if the user asks for a negative page size") {
    forAll { version: ApiVersions.Value =>
      withApiFixtures(apiVersion = version) { case (apiPrefix, _, _, server: EmbeddedHttpServer) =>
        val pageSize = -50
        server.httpGet(
          path = s"/$apiPrefix/works?pageSize=$pageSize",
          andExpect = Status.BadRequest,
          withJsonBody =
            badRequest(apiPrefix, s"pageSize: [$pageSize] is not greater than or equal to 1")
        )
      }
    }
  }

  it("returns a BadRequest error if the user asks for page 0") {
    forAll { version: ApiVersions.Value =>
      withApiFixtures(apiVersion = version) { case (apiPrefix, _, _, server: EmbeddedHttpServer) =>
        server.httpGet(
          path = s"/$apiPrefix/works?page=0",
          andExpect = Status.BadRequest,
          withJsonBody = badRequest(apiPrefix, "page: [0] is not greater than or equal to 1")
        )
      }
    }
  }

  it("returns a BadRequest error if the user asks for a page before 0") {
    forAll { version: ApiVersions.Value =>
      withApiFixtures(apiVersion = version) { case (apiPrefix, _, _, server: EmbeddedHttpServer) =>
        server.httpGet(
          path = s"/$apiPrefix/works?page=-50",
          andExpect = Status.BadRequest,
          withJsonBody =
            badRequest(apiPrefix, "page: [-50] is not greater than or equal to 1")
        )
      }
    }
  }

  it("returns multiple errors if there's more than one invalid parameter") {
    forAll { version: ApiVersions.Value =>
      withApiFixtures(apiVersion = version) { case (apiPrefix, _, _, server: EmbeddedHttpServer) =>
        server.httpGet(
          path = s"/$apiPrefix/works?pageSize=-60&page=-50",
          andExpect = Status.BadRequest,
          withJsonBody = badRequest(apiPrefix,
            "page: [-50] is not greater than or equal to 1, pageSize: [-60] is not greater than or equal to 1")
        )
      }
    }
  }

  it("returns a Bad Request error if asked for an invalid include") {
    forAll { version: ApiVersions.Value =>
      withApiFixtures(apiVersion = version) { case (apiPrefix, _, _, server: EmbeddedHttpServer) =>
        server.httpGet(
          path = s"/$apiPrefix/works?includes=foo",
          andExpect = Status.BadRequest,
          withJsonBody = badRequest(apiPrefix, "includes: 'foo' is not a valid include")
        )
      }
    }
  }

  it("returns a Bad Request error if asked for more than one invalid include") {
    forAll { version: ApiVersions.Value =>
      withApiFixtures(apiVersion = version) { case (apiPrefix, _, _, server: EmbeddedHttpServer) =>
        server.httpGet(
          path = s"/$apiPrefix/works?includes=foo,bar",
          andExpect = Status.BadRequest,
          withJsonBody =
            badRequest(apiPrefix, "includes: 'foo', 'bar' are not valid includes")
        )
      }
    }
  }

  it(
    "returns a Bad Request error if asked for a mixture of valid and invalid includes") {
    forAll { version: ApiVersions.Value =>
      withApiFixtures(apiVersion = version) { case (apiPrefix, _, _, server: EmbeddedHttpServer) =>
        server.httpGet(
          path = s"/$apiPrefix/works?includes=foo,identifiers,bar",
          andExpect = Status.BadRequest,
          withJsonBody =
            badRequest(apiPrefix, "includes: 'foo', 'bar' are not valid includes")
        )
      }
    }
  }

  it(
    "returns a Bad Request error if asked for an invalid include on an individual work") {
    forAll { version: ApiVersions.Value =>
      withApiFixtures(apiVersion = version) { case (apiPrefix, _, _, server: EmbeddedHttpServer) =>
        server.httpGet(
          path = s"/$apiPrefix/works/nfdn7wac?includes=foo",
          andExpect = Status.BadRequest,
          withJsonBody = badRequest(apiPrefix, "includes: 'foo' is not a valid include")
        )
      }
    }
  }

  it("returns Not Found if you look up a non-existent index") {
    forAll { version: ApiVersions.Value =>
      withApiFixtures(apiVersion = version) { case (apiPrefix, _, _, server: EmbeddedHttpServer) => server.httpGet(
        path = s"/$apiPrefix/works?_index=foobarbaz",
        andExpect = Status.NotFound,
        withJsonBody = notFound(apiPrefix, "There is no index foobarbaz")
      )
      }
    }
  }

  it("returns Not Found if you ask for a non-existent work") {
    forAll { version: ApiVersions.Value =>
      withApiFixtures(apiVersion = version) { case (apiPrefix, _, _, server: EmbeddedHttpServer) =>
        server.httpGet(
          path = s"/$apiPrefix/works/xhu96f9j",
          andExpect = Status.NotFound,
          withJsonBody = notFound(apiPrefix, "Work not found for identifier xhu96f9j")
        )
      }
    }
  }

  it("returns Bad Request if you ask for a malformed identifier") {
    forAll { version: ApiVersions.Value =>
      withApiFixtures(apiVersion = version) { case (apiPrefix, _, _, server: EmbeddedHttpServer) =>
        server.httpGet(
          path = s"/$apiPrefix/works/zd224ncv]",
          andExpect = Status.BadRequest,
          withJsonBody =
            badRequest(apiPrefix, "Unrecognised character in identifier zd224ncv]")
        )
      }
    }
  }

  it("returns an Internal Server error if you try to search a malformed index") {
    // We need to do something that reliably triggers an internal exception
    // in the Elasticsearch handler.
    //
    // Elasticsearch has a number of "private" indexes, which don't have
    // a canonicalId field to sort on.  Trying to query one of these will
    // trigger one such exception!
    forAll { version: ApiVersions.Value =>
      withApiFixtures(apiVersion = version)  { case (apiPrefix, _, _, server: EmbeddedHttpServer) =>
      server.httpGet(
        path = s"/$apiPrefix/works?_index=.watches",
        andExpect = Status.InternalServerError,
        withJsonBody = s"""{
          "@context": "https://localhost:8888/$apiPrefix/context.json",
          "type": "Error",
          "errorType": "http",
          "httpStatus": 500,
          "label": "Internal Server Error"
        }"""
      )
    }
    }
  }

  it(
    "returns a Bad Request error if you try to access the 10000th page") {
    forAll { version: ApiVersions.Value =>
      withApiFixtures(apiVersion = version) { case (apiPrefix, _, _, server: EmbeddedHttpServer) =>
        server.httpGet(
          path = s"/$apiPrefix/works?page=10000",
          andExpect = Status.BadRequest,
          withJsonBody =
            badRequest(apiPrefix, "Only the first 10000 works are available in the API.")
        )
      }
    }
    }

  it(
    "returns a Bad Request error if you try to get the 101th page with 100 results per page") {
    forAll { version: ApiVersions.Value =>
      withApiFixtures(apiVersion = version) { case (apiPrefix, _, _, server: EmbeddedHttpServer) =>
        server.httpGet(
          path = s"/$apiPrefix/works?pageSize=100&page=101",
          andExpect = Status.BadRequest,
          withJsonBody =
            badRequest(apiPrefix, "Only the first 10000 works are available in the API.")
        )
      }
    }
  }

  // TODO figure out what the correct behaviour should be in this case
  ignore(
    "returns a Not Found error if you try to get a version that doesn't exist") {
      withServer(indexName = "not-important") { server =>
        server.httpGet(
          path = "/catalogue/v567/works?pageSize=100&page=101",
          andExpect = Status.NotFound,
          withJsonBody =
            badRequest(s"catalogue/${ApiVersions.default.toString}", "v567 is not a valid API version")
        )
      }
  }
}
