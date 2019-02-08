package uk.ac.wellcome.platform.api.works.v1

import com.sksamuel.elastic4s.Index
import com.twitter.finatra.http.EmbeddedHttpServer
import uk.ac.wellcome.display.models.ApiVersions
import uk.ac.wellcome.display.models.v1.DisplayV1SerialisationTestBase
import uk.ac.wellcome.platform.api.works.ApiWorksTestBase
import uk.ac.wellcome.fixtures.TestWith

trait ApiV1WorksTestBase
    extends ApiWorksTestBase
    with DisplayV1SerialisationTestBase {
  val apiPrefix: String = getApiPrefix(ApiVersions.v1)

  def withV1Api[R](testWith: TestWith[(Index, EmbeddedHttpServer), R]): R =
    withLocalWorksIndex { indexV1 =>
      withServer(indexV1, Index("index-v2")) { server =>
        testWith((indexV1, server))
      }
    }
}
