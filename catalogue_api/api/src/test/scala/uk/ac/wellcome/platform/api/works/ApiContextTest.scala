package uk.ac.wellcome.platform.api.works

import com.twitter.finagle.http.Status
import com.twitter.finatra.http.EmbeddedHttpServer
import org.apache.commons.io.IOUtils
import uk.ac.wellcome.versions.ApiVersions

class ApiContextTest extends ApiWorksTestBase {

  it("returns a context for all versions") {
    ApiVersions.values.toList.foreach { version: ApiVersions.Value =>
      withApiFixtures(apiVersion = version) {
        case (apiPrefix, _, _, server: EmbeddedHttpServer) =>
          server.httpGet(
            path = s"/$apiPrefix/context.json",
            andExpect = Status.Ok,
            withJsonBody = IOUtils.toString(getClass.getResourceAsStream("/context.json")))
      }
    }
  }

}
