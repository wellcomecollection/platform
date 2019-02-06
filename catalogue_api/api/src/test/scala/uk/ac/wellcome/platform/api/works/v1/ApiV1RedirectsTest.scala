package uk.ac.wellcome.platform.api.works.v1

import com.twitter.finagle.http.Status
import com.twitter.finatra.http.EmbeddedHttpServer

class ApiV1RedirectsTest extends ApiV1WorksTestBase {
  it("returns a 302 Redirect if looking up a redirected work") {
    val redirectedWork = createIdentifiedRedirectedWork

    withV1Api {
      case (indexV1, server: EmbeddedHttpServer) =>
        insertIntoElasticsearch(indexV1, redirectedWork)
        server.httpGet(
          path = s"/$apiPrefix/works/${redirectedWork.canonicalId}",
          andExpect = Status.Found,
          withBody = "",
          withLocation =
            s"/$apiPrefix/works/${redirectedWork.redirect.canonicalId}"
        )
    }
  }

  it("preserves query parameters on a 302 Redirect") {
    val redirectedWork = createIdentifiedRedirectedWork

    withV1Api {
      case (indexV1, server: EmbeddedHttpServer) =>
        insertIntoElasticsearch(indexV1, redirectedWork)
        server.httpGet(
          path =
            s"/$apiPrefix/works/${redirectedWork.canonicalId}?includes=identifiers",
          andExpect = Status.Found,
          withBody = "",
          withLocation =
            s"/$apiPrefix/works/${redirectedWork.redirect.canonicalId}?includes=identifiers"
        )
    }
  }
}
