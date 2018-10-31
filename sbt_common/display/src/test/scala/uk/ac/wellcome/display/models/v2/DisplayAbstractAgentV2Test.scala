package uk.ac.wellcome.display.models.v2

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.work.generators.IdentifiersGenerators
import uk.ac.wellcome.models.work.internal._

class DisplayAbstractAgentV2Test
    extends FunSpec
    with Matchers
    with IdentifiersGenerators {

  val label: String = randomAlphanumeric(length = 25)
  val sourceIdentifier: SourceIdentifier = createSourceIdentifier
  val otherIdentifiers: List[SourceIdentifier] = (1 to 3).map { _ =>
    createSourceIdentifier
  }.toList
  val canonicalId: String = createCanonicalId

  describe("Agent") {
    val agent = Agent(label = label)

    val unidentifiedAgent = Unidentifiable(agent)

    val identifiedAgent = Identified(
      canonicalId = canonicalId,
      sourceIdentifier = sourceIdentifier,
      otherIdentifiers = otherIdentifiers,
      agent = agent
    )

    val expectedUnidentifiedAgent: DisplayAgentV2 = DisplayAgentV2(
      id = None,
      identifiers = None,
      label = label
    )

    it(
      "converts an Unidentifiable Agent to a DisplayAgentV2 (includesIdentifiers = true)") {
      DisplayAbstractAgentV2(unidentifiedAgent, includesIdentifiers = true) shouldBe expectedUnidentifiedAgent
    }

    it(
      "converts an Unidentifiable Agent to a DisplayAgentV2 (includesIdentifiers = false)") {
      DisplayAbstractAgentV2(unidentifiedAgent, includesIdentifiers = false) shouldBe expectedUnidentifiedAgent
    }

    it(
      "converts an Identified Agent to a DisplayAgentV2 (includesIdentifiers = true)") {
      val expectedAgent = DisplayAgentV2(
        id = Some(canonicalId),
        identifiers = Some((List(sourceIdentifier) ++ otherIdentifiers).map {
          DisplayIdentifierV2(_)
        }),
        label = label
      )

      DisplayAbstractAgentV2(identifiedAgent, includesIdentifiers = true) shouldBe expectedAgent
    }

    it(
      "converts an Identified Agent to a DisplayAgentV2 (includesIdentifiers = false)") {
      val expectedAgent = DisplayAgentV2(
        id = Some(canonicalId),
        identifiers = None,
        label = label
      )

      DisplayAbstractAgentV2(identifiedAgent, includesIdentifiers = false) shouldBe expectedAgent
    }
  }

  describe("Person") {
    val prefix = randomAlphanumeric(length = 5)
    val numeration = randomAlphanumeric(length = 3)

    val person = Person(
      label = label,
      prefix = Some(prefix),
      numeration = Some(numeration)
    )

    val unidentifiedPerson = Unidentifiable(person)

    val identifiedAgent = Identified(
      canonicalId = canonicalId,
      sourceIdentifier = sourceIdentifier,
      otherIdentifiers = otherIdentifiers,
      agent = person
    )

    val expectedUnidentifiedPerson: DisplayPersonV2 = DisplayPersonV2(
      id = None,
      identifiers = None,
      label = label,
      prefix = Some(prefix),
      numeration = Some(numeration)
    )

    it(
      "converts an Unidentifiable Person to a DisplayPersonV2 (includesIdentifiers = true)") {
      DisplayAbstractAgentV2(unidentifiedPerson, includesIdentifiers = true) shouldBe expectedUnidentifiedPerson
    }

    it(
      "converts an Unidentifiable Person to a DisplayPersonV2 (includesIdentifiers = false)") {
      DisplayAbstractAgentV2(unidentifiedPerson, includesIdentifiers = false) shouldBe expectedUnidentifiedPerson
    }

    it(
      "converts an Identified Person to a DisplayPersonV2 (includesIdentifiers = true)") {
      val expectedPerson = DisplayPersonV2(
        id = Some(canonicalId),
        identifiers = Some((List(sourceIdentifier) ++ otherIdentifiers).map {
          DisplayIdentifierV2(_)
        }),
        label = label,
        prefix = Some(prefix),
        numeration = Some(numeration)
      )

      DisplayAbstractAgentV2(identifiedAgent, includesIdentifiers = true) shouldBe expectedPerson
    }

    it(
      "converts an Identified Person to a DisplayPersonV2 (includesIdentifiers = false)") {
      val expectedPerson = DisplayPersonV2(
        id = Some(canonicalId),
        identifiers = None,
        label = label,
        prefix = Some(prefix),
        numeration = Some(numeration)
      )

      DisplayAbstractAgentV2(identifiedAgent, includesIdentifiers = false) shouldBe expectedPerson
    }
  }

  describe("Organisation") {
    val organisation = Organisation(label = label)

    val unidentifiedAgent = Unidentifiable(organisation)

    val identifiedAgent = Identified(
      canonicalId = canonicalId,
      sourceIdentifier = sourceIdentifier,
      otherIdentifiers = otherIdentifiers,
      agent = organisation
    )

    val expectedUnidentifiedOrganisation: DisplayOrganisationV2 =
      DisplayOrganisationV2(
        id = None,
        identifiers = None,
        label = label
      )

    it(
      "converts an Unidentifiable Organisation to a DisplayOrganisationV2 (includesIdentifiers = true)") {
      DisplayAbstractAgentV2(unidentifiedAgent, includesIdentifiers = true) shouldBe expectedUnidentifiedOrganisation
    }

    it(
      "converts an Unidentifiable Organisation to a DisplayOrganisationV2 (includesIdentifiers = false)") {
      DisplayAbstractAgentV2(unidentifiedAgent, includesIdentifiers = false) shouldBe expectedUnidentifiedOrganisation
    }

    it(
      "converts an Identified Organisation to a DisplayOrganisationV2 (includesIdentifiers = true)") {
      val expectedOrganisation = DisplayOrganisationV2(
        id = Some(canonicalId),
        identifiers = Some((List(sourceIdentifier) ++ otherIdentifiers).map {
          DisplayIdentifierV2(_)
        }),
        label = label
      )

      DisplayAbstractAgentV2(identifiedAgent, includesIdentifiers = true) shouldBe expectedOrganisation
    }

    it(
      "converts an Identified Organisation to a DisplayOrganisationV2 (includesIdentifiers = false)") {
      val expectedOrganisation = DisplayOrganisationV2(
        id = Some(canonicalId),
        identifiers = None,
        label = label
      )

      DisplayAbstractAgentV2(identifiedAgent, includesIdentifiers = false) shouldBe expectedOrganisation
    }
  }
}
