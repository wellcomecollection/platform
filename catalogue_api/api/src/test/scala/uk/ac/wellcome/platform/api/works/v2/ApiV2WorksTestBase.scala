package uk.ac.wellcome.platform.api.works.v2

import uk.ac.wellcome.display.models.ApiVersions
import uk.ac.wellcome.display.models.v2.DisplayV2SerialisationTestBase
import uk.ac.wellcome.platform.api.works.ApiWorksTestBase

trait ApiV2WorksTestBase
    extends ApiWorksTestBase
    with DisplayV2SerialisationTestBase {
  def withV2Api[R] = withApiFixtures[R](ApiVersions.v2)(_)
}
