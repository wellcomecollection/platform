package uk.ac.wellcome.platform.api.works

import com.sksamuel.elastic4s.Index
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
        withJsonBody = notFound(
          s"catalogue/${ApiVersions.default.toString}",
          "Page not found for URL /catalogue/v567/works")
      )
    }
  }

  it("returns a Not Found error if you try to get an unrecognised path") {
    withServer { server =>
      server.httpGet(
        path = "/foo/bar",
        andExpect = Status.NotFound,
        withJsonBody = notFound(
          s"catalogue/${ApiVersions.default.toString}",
          "Page not found for URL /foo/bar")
      )
    }
  }

  private def withServer[R](testWith: TestWith[EmbeddedHttpServer, R]): R =
    withServer(indexV1 = Index("index-v1"), indexV2 = Index("index-v2")) {
      server =>
        testWith(server)
    }
}
