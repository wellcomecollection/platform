package uk.ac.wellcome.display.models.v2

import org.scalatest.Assertion
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.work.internal.{Agent, Unidentifiable}
import uk.ac.wellcome.models.work.test.util.IdentifiersUtil

class DisplayAbstractAgentV2Test extends FunSpec with Matchers with IdentifiersUtil {

  describe("Agent") {
    it("converts an Unidentifiable Agent to an AbstractAgent (includesIdentifiers = true)") {
      checkUnidentifiableAgent(includesIdentifiers = true)
    }

    it("converts an Unidentifiable Agent to an AbstractAgent (includesIdentifiers = false)") {
      checkUnidentifiableAgent(includesIdentifiers = false)
    }
  }

  private def checkUnidentifiableAgent(includesIdentifiers: Boolean): Assertion = {
    val label = createLabel
    val agent = Unidentifiable(Agent(label = label))
    val expectedAgent = DisplayAgentV2(
      id = None,
      identifiers = None,
      label = label
    )

    DisplayAbstractAgentV2(agent, includesIdentifiers = includesIdentifiers) shouldBe expectedAgent
  }

  private def createLabel: String = randomAlphanumeric(length = 25)
}
