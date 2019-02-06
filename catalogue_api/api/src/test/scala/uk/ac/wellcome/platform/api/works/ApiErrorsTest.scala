package uk.ac.wellcome.platform.api.works

import com.twitter.finagle.http.Status
import com.twitter.finatra.http.EmbeddedHttpServer

class ApiErrorsTest extends ApiWorksTestBase {

  it("returns a Not Found error if you try to get an API version") {
    withHttpServer { server: EmbeddedHttpServer =>
      server.httpGet(
        path = "/catalogue/v567/works",
        andExpect = Status.NotFound,
        withJsonBody = notFound(
          getApiPrefix(),
          "Page not found for URL /catalogue/v567/works")
      )
    }
  }

  it("returns a Not Found error if you try to get an unrecognised path") {
    withHttpServer { server: EmbeddedHttpServer =>
      server.httpGet(
        path = "/foo/bar",
        andExpect = Status.NotFound,
        withJsonBody =
          notFound(getApiPrefix(), "Page not found for URL /foo/bar")
      )
    }
  }
}
