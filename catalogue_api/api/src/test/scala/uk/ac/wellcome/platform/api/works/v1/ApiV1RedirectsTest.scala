package uk.ac.wellcome.platform.api.works.v1

import com.twitter.finagle.http.Status
import com.twitter.finatra.http.EmbeddedHttpServer

class ApiV1RedirectsTest extends ApiV1WorksTestBase {
  it("returns a TemporaryRedirect if looking up a redirected work") {
    val redirectedWork = createIdentifiedRedirectedWork

    withV1Api {
      case (apiPrefix, indexNameV1, _, itemType, server: EmbeddedHttpServer) =>
        insertIntoElasticsearch(indexNameV1, itemType, redirectedWork)
        server.httpGet(
          path = s"/$apiPrefix/works/${redirectedWork.canonicalId}",
          andExpect = Status.TemporaryRedirect,
          withBody = "",
          withLocation = s"/$apiPrefix/works/${redirectedWork.redirect.canonicalId}"
        )
    }
  }
}
