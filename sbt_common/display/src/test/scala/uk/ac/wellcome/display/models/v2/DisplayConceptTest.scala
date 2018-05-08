package uk.ac.wellcome.display.models.v2

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.display.models._
import uk.ac.wellcome.models.work.internal._

class DisplayConceptTest extends FunSpec with Matchers {

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
    val sourceIdentifier = SourceIdentifier(
      identifierScheme = IdentifierSchemes.libraryOfCongressNames,
      ontologyType = "Concept",
      value = "lcsh/ehw"
    )

    assertDisplayConceptIsCorrect(
      concept = Identified(
        id = "dj4kndg5",
        identifiers = List(sourceIdentifier),
        agent = Concept(label = "darkness")
      ),
      expectedDisplayConcept = DisplayPeriod(
        id = Some("dj4kndg5"),
        identifiers = Some(List(DisplayIdentifier(sourceIdentifier))),
        label = "darkness"
      )
    )
  }

  it("reads an identified Period as a DisplayPeriod with identifiers") {
    val sourceIdentifier = SourceIdentifier(
      identifierScheme = IdentifierSchemes.libraryOfCongressNames,
      ontologyType = "Period",
      value = "lcsh/zbm"
    )

    assertDisplayPeriodIsCorrect(
      concept = Identified(
        id = "nrzbm3ah",
        identifiers = List(sourceIdentifier),
        agent = Period(label = "never")
      ),
      expectedDisplayPeriod = DisplayPeriod(
        id = Some("nrzbm3ah"),
        identifiers = Some(List(DisplayIdentifier(sourceIdentifier))),
        label = "never"
      )
    )
  }

  it("reads an identified Place as a DisplayPlace with identifiers") {
    val sourceIdentifier = SourceIdentifier(
      identifierScheme = IdentifierSchemes.libraryOfCongressNames,
      ontologyType = "Place",
      value = "lcsh/axt"
    )

    assertDisplayPlaceIsCorrect(
      concept = Identified(
        id = "axtswq4z",
        identifiers = List(sourceIdentifier),
        agent = Place(label = "anywhere")
      ),
      expectedDisplayPlace = DisplayPlace(
        id = Some("axtswq4z"),
        identifiers = Some(List(DisplayIdentifier(sourceIdentifier))),
        label = "anywhere"
      )
    )
  }

  private def assertDisplayConceptIsCorrect(
    concept: Displayable[Concept],
    expectedDisplayConcept: DisplayAbstractConcept
  ) = {
    val displayConcept = DisplayAbstractConcept(concept)
    displayConcept shouldBe expectedDisplayConcept
  }
}
