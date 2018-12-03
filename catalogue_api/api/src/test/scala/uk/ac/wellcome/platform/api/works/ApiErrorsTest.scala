package uk.ac.wellcome.platform.api.works

import com.twitter.finagle.http.Status
import com.twitter.finatra.http.EmbeddedHttpServer
import uk.ac.wellcome.display.models.ApiVersions
import uk.ac.wellcome.test.fixtures.TestWith

class ApiErrorsTest extends ApiWorksTestBase {

  it("returns a Not Found error if you try to get an API version") {
    withServer() { server =>
      server.httpGet(
        path = "/catalogue/v567/works",
        andExpect = Status.NotFound,
        withJsonBody = notFound(
          s"catalogue/${ApiVersions.default.toString}",
          "Page not found for URL /catalogue/v567/works")
      )
    }
  }

  it("returns a Not Found error if you try to get an unrecognised path") {
    withServer() { server =>
      server.httpGet(
        path = "/foo/bar",
        andExpect = Status.NotFound,
        withJsonBody = notFound(
          s"catalogue/${ApiVersions.default.toString}",
          "Page not found for URL /foo/bar")
      )
    }
  }
}
