package uk.ac.wellcome.display.models.v2

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.work.internal.{Agent, Identified, SourceIdentifier, Unidentifiable}
import uk.ac.wellcome.models.work.test.util.IdentifiersUtil

class DisplayAbstractAgentV2Test extends FunSpec with Matchers with IdentifiersUtil {

  val label: String = randomAlphanumeric(length = 25)
  val sourceIdentifier: SourceIdentifier = createSourceIdentifier
  val otherIdentifiers: List[SourceIdentifier] = (1 to 3).map { _ => createSourceIdentifier }.toList
  val canonicalId: String = createCanonicalId

  describe("Agent") {
    val unidentifiedAgent = Unidentifiable(Agent(label = label))

    val identifiedAgent = Identified(
      canonicalId = canonicalId,
      sourceIdentifier = sourceIdentifier,
      otherIdentifiers = otherIdentifiers,
      agent = Agent(label = label)
    )

    val expectedUnidentifiedAgent: DisplayAgentV2 = DisplayAgentV2(
      id = None,
      identifiers = None,
      label = label
    )

    it("converts an Unidentifiable Agent to an AbstractAgent (includesIdentifiers = true)") {
      DisplayAbstractAgentV2(
        unidentifiedAgent,
        includesIdentifiers = true) shouldBe expectedUnidentifiedAgent
    }

    it("converts an Unidentifiable Agent to an AbstractAgent (includesIdentifiers = false)") {
      DisplayAbstractAgentV2(
        unidentifiedAgent,
        includesIdentifiers = false) shouldBe expectedUnidentifiedAgent
    }

    it("converts an Identified Agent to an AbstractAgent (includesIdentifiers = true)") {
      val expectedAgent = DisplayAgentV2(
        id = Some(canonicalId),
        identifiers = Some((List(sourceIdentifier) ++ otherIdentifiers).map { DisplayIdentifierV2(_) }),
        label = label
      )

      DisplayAbstractAgentV2(
        identifiedAgent,
        includesIdentifiers = true) shouldBe expectedAgent
    }

    it("converts an Identified Agent to an AbstractAgent (includesIdentifiers = false)") {
      val expectedAgent = DisplayAgentV2(
        id = Some(canonicalId),
        identifiers = None,
        label = label
      )

      DisplayAbstractAgentV2(
        identifiedAgent,
        includesIdentifiers = false) shouldBe expectedAgent
    }
  }
}
