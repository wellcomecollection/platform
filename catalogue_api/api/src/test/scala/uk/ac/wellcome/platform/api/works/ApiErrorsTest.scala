package uk.ac.wellcome.platform.api.works

import com.twitter.finagle.http.Status
import com.twitter.finatra.http.EmbeddedHttpServer
import uk.ac.wellcome.display.models.ApiVersions

class ApiErrorsTest extends ApiWorksTestBase {

  it("returns a Not Found error if you try to get an API version") {
    withHttpServer(ApiVersions.default) {
      case (apiPrefix, server: EmbeddedHttpServer) =>
        server.httpGet(
          path = "/catalogue/v567/works",
          andExpect = Status.NotFound,
          withJsonBody = notFound(
            apiPrefix,
            "Page not found for URL /catalogue/v567/works")
        )
    }
  }

  it("returns a Not Found error if you try to get an unrecognised path") {
    withHttpServer(ApiVersions.default) {
      case (apiPrefix, server: EmbeddedHttpServer) =>
        server.httpGet(
          path = "/foo/bar",
          andExpect = Status.NotFound,
          withJsonBody = notFound(
            apiPrefix,
            "Page not found for URL /foo/bar")
        )
    }
  }
}
