package uk.ac.wellcome.platform.api.works.v2

import com.sksamuel.elastic4s.Index
import com.twitter.finatra.http.EmbeddedHttpServer
import uk.ac.wellcome.display.models.ApiVersions
import uk.ac.wellcome.display.models.v2.DisplayV2SerialisationTestBase
import uk.ac.wellcome.platform.api.works.ApiWorksTestBase
import uk.ac.wellcome.fixtures.TestWith

trait ApiV2WorksTestBase
    extends ApiWorksTestBase
    with DisplayV2SerialisationTestBase {
  val apiPrefix: String = getApiPrefix(ApiVersions.v2)

  def withV2Api[R](testWith: TestWith[(Index, EmbeddedHttpServer), R]): R =
    withLocalWorksIndex { indexV2 =>
      withServer(Index("index-v1"), indexV2) { server =>
        testWith((indexV2, server))
      }
    }
}
