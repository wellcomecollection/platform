package uk.ac.wellcome.platform.api.works

import com.twitter.finagle.http.Status
import uk.ac.wellcome.models.Work

class PublishersTest extends ApiWorksTestBase {

  it("includes an empty publishers field if the work has no publishers") { }

  it("includes the publishers field for agent publishers") { }

  it("includes the publishers field with a mixture of agents/organisations") { }
}
