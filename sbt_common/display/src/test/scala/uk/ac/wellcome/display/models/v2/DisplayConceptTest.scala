package uk.ac.wellcome.display.models.v2

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.work.generators.IdentifiersGenerators
import uk.ac.wellcome.models.work.internal._

class DisplayConceptTest
    extends FunSpec
    with Matchers
    with IdentifiersGenerators {

  it("reads an unidentified generic Concept as a DisplayConcept") {
    assertDisplayConceptIsCorrect(
      concept = Unidentifiable(Concept(label = "evil")),
      expectedDisplayConcept = DisplayConcept(
        id = None,
        identifiers = None,
        label = "evil"
      )
    )
  }

  it("reads an unidentified Period as a DisplayPeriod") {
    assertDisplayConceptIsCorrect(
      concept = Unidentifiable(Period(label = "darkness")),
      expectedDisplayConcept = DisplayPeriod(
        id = None,
        identifiers = None,
        label = "darkness"
      )
    )
  }

  it("reads an unidentified Place as a DisplayPlace") {
    assertDisplayConceptIsCorrect(
      concept = Unidentifiable(Place(label = "nowhere")),
      expectedDisplayConcept = DisplayPlace(
        id = None,
        identifiers = None,
        label = "nowhere"
      )
    )
  }

  it("reads an identified Concept as a DisplayConcept with identifiers") {
    val sourceIdentifier = createSourceIdentifierWith(
      ontologyType = "Concept"
    )

    assertDisplayConceptIsCorrect(
      concept = Identified(
        canonicalId = "dj4kndg5",
        sourceIdentifier = sourceIdentifier,
        agent = Concept(label = "darkness")
      ),
      expectedDisplayConcept = DisplayConcept(
        id = Some("dj4kndg5"),
        identifiers = Some(List(DisplayIdentifierV2(sourceIdentifier))),
        label = "darkness"
      )
    )
  }

  it("reads an identified Period as a DisplayPeriod with identifiers") {
    val sourceIdentifier = createSourceIdentifierWith(
      ontologyType = "Period"
    )

    assertDisplayConceptIsCorrect(
      concept = Identified(
        canonicalId = "nrzbm3ah",
        sourceIdentifier = sourceIdentifier,
        agent = Period(label = "never")
      ),
      expectedDisplayConcept = DisplayPeriod(
        id = Some("nrzbm3ah"),
        identifiers = Some(List(DisplayIdentifierV2(sourceIdentifier))),
        label = "never"
      )
    )
  }

  it("reads an identified Place as a DisplayPlace with identifiers") {
    val sourceIdentifier = createSourceIdentifierWith(
      ontologyType = "Place"
    )

    assertDisplayConceptIsCorrect(
      concept = Identified(
        canonicalId = "axtswq4z",
        sourceIdentifier = sourceIdentifier,
        agent = Place(label = "anywhere")
      ),
      expectedDisplayConcept = DisplayPlace(
        id = Some("axtswq4z"),
        identifiers = Some(List(DisplayIdentifierV2(sourceIdentifier))),
        label = "anywhere"
      )
    )
  }

  private def assertDisplayConceptIsCorrect(
    concept: Displayable[AbstractConcept],
    expectedDisplayConcept: DisplayAbstractConcept
  ) = {
    val displayConcept =
      DisplayAbstractConcept(concept, includesIdentifiers = true)
    displayConcept shouldBe expectedDisplayConcept
  }
}
