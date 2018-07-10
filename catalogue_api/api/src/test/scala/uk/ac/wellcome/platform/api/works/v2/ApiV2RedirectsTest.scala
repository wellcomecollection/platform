package uk.ac.wellcome.platform.api.works.v2

import com.twitter.finagle.http.Status
import com.twitter.finatra.http.EmbeddedHttpServer

class ApiV2RedirectsTest extends ApiV2WorksTestBase {
  it("returns a TemporaryRedirect if looking up a redirected work") {
    val redirectedWork = createIdentifiedRedirectedWork

    withV2Api {
      case (apiPrefix, _, indexNameV2, itemType, server: EmbeddedHttpServer) =>
        insertIntoElasticsearch(indexNameV2, itemType, redirectedWork)
        server.httpGet(
          path = s"/$apiPrefix/works/${redirectedWork.canonicalId}",
          andExpect = Status.Found,
          withBody = "",
          withLocation = s"/$apiPrefix/works/${redirectedWork.redirect.canonicalId}"
        )
    }
  }

  it("preserves query parameters on a 302 Redirect") {
    val redirectedWork = createIdentifiedRedirectedWork

    withV2Api {
      case (apiPrefix, _, indexNameV2, itemType, server: EmbeddedHttpServer) =>
        insertIntoElasticsearch(indexNameV2, itemType, redirectedWork)
        server.httpGet(
          path = s"/$apiPrefix/works/${redirectedWork.canonicalId}?includes=identifiers",
          andExpect = Status.Found,
          withBody = "",
          withLocation = s"/$apiPrefix/works/${redirectedWork.redirect.canonicalId}?includes=identifiers"
        )
    }
  }
}
