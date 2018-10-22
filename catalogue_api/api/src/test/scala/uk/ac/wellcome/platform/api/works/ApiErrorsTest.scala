package uk.ac.wellcome.platform.api.works

import com.twitter.finagle.http.Status
import com.twitter.finatra.http.EmbeddedHttpServer
import uk.ac.wellcome.display.models.ApiVersions
import uk.ac.wellcome.test.fixtures.TestWith

class ApiErrorsTest extends ApiWorksTestBase {

  it("returns a Not Found error if you try to get an API version") {
    withServer { server =>
      server.httpGet(
        path = "/catalogue/v567/works",
        andExpect = Status.NotFound,
        withJsonBody = badRequest(
          s"catalogue/${ApiVersions.default.toString}",
          "v567 is not a valid API version")
      )
    }
  }

  it("returns a Not Found error if you try to get an unrecognised path") {
    withServer { server =>
      server.httpGet(
        path = "/foo/bar",
        andExpect = Status.NotFound,
        withJsonBody = badRequest(
          s"catalogue/${ApiVersions.default.toString}",
          "v567 is not a valid API version")
      )
    }
  }

  private def withServer[R](testWith: TestWith[EmbeddedHttpServer, R]): R =
    withServer(indexNameV1 = "index-v1", indexNameV2 = "index-v2") { server =>
      testWith(server)
    }
}
