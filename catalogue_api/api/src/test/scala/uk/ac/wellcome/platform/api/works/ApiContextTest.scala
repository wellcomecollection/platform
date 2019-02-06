package uk.ac.wellcome.platform.api.works

import com.twitter.finagle.http.Status
import com.twitter.finatra.http.EmbeddedHttpServer
import org.apache.commons.io.IOUtils
import uk.ac.wellcome.display.models.ApiVersions

class ApiContextTest extends ApiWorksTestBase {
  it("returns a context for all versions") {
    withHttpServer { server: EmbeddedHttpServer =>
      ApiVersions.values.toList.foreach { apiVersion: ApiVersions.Value =>
        server.httpGet(
          path = s"/${getApiPrefix(apiVersion)}/context.json",
          andExpect = Status.Ok,
          withJsonBody = IOUtils
            .toString(getClass.getResourceAsStream("/context.json"), "UTF-8")
        )
      }
    }
  }
}
