package uk.ac.wellcome.platform.api.works.v1

import uk.ac.wellcome.display.models.ApiVersions
import uk.ac.wellcome.display.models.v1.DisplayV1SerialisationTestBase
import uk.ac.wellcome.platform.api.works.ApiWorksTestBase

trait ApiV1WorksTestBase
    extends ApiWorksTestBase
    with DisplayV1SerialisationTestBase {
  val apiVersion: ApiVersions.Value = ApiVersions.v1
}
