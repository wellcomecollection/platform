package uk.ac.wellcome.display.models.v1

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.work.generators.IdentifiersGenerators
import uk.ac.wellcome.models.work.internal.{Agent, Identified}

class DisplayAgentV1Test
    extends FunSpec
    with Matchers
    with IdentifiersGenerators {
  it("errors if you try to serialise from an identified Agent") {
    val agent = Identified(
      agent = Agent(label = "Henry Wellcome"),
      canonicalId = createCanonicalId,
      sourceIdentifier = createSourceIdentifierWith(
        ontologyType = "Agent"
      ),
      otherIdentifiers = List()
    )

    val caught = intercept[IllegalArgumentException] {
      DisplayAgentV1(agent)
    }

    caught.getMessage shouldBe s"Unexpectedly asked to convert identified agent $agent to DisplayAgentV1"
  }
}
