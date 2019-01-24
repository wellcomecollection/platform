package uk.ac.wellcome.display.models.v1

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.display.models._
import uk.ac.wellcome.models.work.generators.{
  ProductionEventGenerators,
  WorksGenerators
}
import uk.ac.wellcome.models.work.internal._

class DisplayWorkV1Test
    extends FunSpec
    with Matchers
    with ProductionEventGenerators
    with WorksGenerators {

  it("parses a Work without any items") {
    val work = createIdentifiedWorkWith(
      items = List()
    )

    val displayWork = DisplayWorkV1(
      work = work,
      includes = V1WorksIncludes(items = true)
    )
    displayWork.items shouldBe Some(List())
  }

  it("parses items on a work") {
    val item = createIdentifiedItemWith(locations = List())
    val work = createIdentifiedWorkWith(
      itemsV1 = List(item)
    )

    val displayWork = DisplayWorkV1(
      work = work,
      includes = V1WorksIncludes(items = true)
    )
    val displayItem = displayWork.items.get.head
    displayItem.id shouldBe item.canonicalId
  }

  it("parses a work without any extra identifiers") {
    val work = createIdentifiedWorkWith(
      otherIdentifiers = List()
    )

    val displayWork = DisplayWorkV1(
      work = work,
      includes = V1WorksIncludes(identifiers = true)
    )
    displayWork.identifiers shouldBe Some(
      List(DisplayIdentifierV1(work.sourceIdentifier)))
  }

  it("extracts creators from a Work with Unidentifiable Contributors") {
    val work = createIdentifiedWorkWith(
      contributors = List(
        Contributor(
          agent = Unidentifiable(Agent(label = "Esmerelda Weatherwax"))
        ),
        Contributor(
          agent = Unidentifiable(Agent("Juniper Journals"))
        )
      )
    )

    val displayWork = DisplayWorkV1(work)
    displayWork.creators shouldBe List(
      DisplayAgentV1(label = "Esmerelda Weatherwax"),
      DisplayAgentV1(label = "Juniper Journals")
    )
  }

  it("gets the physicalDescription from a Work") {
    val physicalDescription = "A magnificent mural of magpies"

    val work = createIdentifiedWorkWith(
      physicalDescription = Some(physicalDescription)
    )

    val displayWork = DisplayWorkV1(work)
    displayWork.physicalDescription shouldBe Some(physicalDescription)
  }

  it("gets the workType from a Work") {
    val workType = WorkType(
      id = "id",
      label = "Proud pooch pavement plops"
    )

    val expectedDisplayWorkType = DisplayWorkType(
      id = workType.id,
      label = workType.label
    )

    val work = createIdentifiedWorkWith(
      workType = Some(workType)
    )

    val displayWork = DisplayWorkV1(work)
    displayWork.workType shouldBe Some(expectedDisplayWorkType)
  }

  it("gets the extent from a Work") {
    val extent = "Bound in boxes of bark"

    val work = createIdentifiedWorkWith(
      extent = Some(extent)
    )

    val displayWork = DisplayWorkV1(work)
    displayWork.extent shouldBe Some(extent)
  }

  it("gets the language from a Work") {
    val language = Language(
      id = "bsl",
      label = "British Sign Language"
    )

    val work = createIdentifiedWorkWith(
      language = Some(language)
    )

    val displayWork = DisplayWorkV1(work)
    val displayLanguage = displayWork.language.get
    displayLanguage.id shouldBe language.id
    displayLanguage.label shouldBe language.label
  }

  it("treats genres as a list of Concepts") {
    val concepts = List(
      Concept("a genre concept"),
      Place("a genre place"),
      Period("a genre period"),
      Concept("a second generic concept")
    )

    val genres = List[Genre[Displayable[AbstractConcept]]](
      Genre(
        label = "a genre created by DisplayWorkV1Test",
        concepts = List(
          Unidentifiable(concepts(0)),
          Unidentifiable(concepts(1)),
          Unidentifiable(concepts(2))
        )
      ),
      Genre(
        label = "a second genre created for DisplayWorkV1Test",
        concepts = List(
          Unidentifiable(concepts(3))
        )
      )
    )

    val work = createIdentifiedWorkWith(
      genres = genres
    )

    val displayWork = DisplayWorkV1(work)
    val expectedGenres = concepts
      .map { c: AbstractConcept =>
        DisplayConceptV1(c.label)
      }
    displayWork.genres shouldBe expectedGenres
  }

  it("treats subjects as a list of Concepts") {
    val concepts = List(
      Concept("a subject concept"),
      Place("a subject place"),
      Period("a subject period"),
      Concept("a second generic concept")
    )

    val subjects = List[Displayable[Subject[Displayable[AbstractConcept]]]](
      Unidentifiable(
        Subject(
          label = "a subject created by DisplayWorkV1Test",
          concepts = List(
            Unidentifiable(concepts(0)),
            Unidentifiable(concepts(1)),
            Unidentifiable(concepts(2))
          )
        )
      ),
      Unidentifiable(
        Subject(
          label = "a second subject created for DisplayWorkV1Test",
          concepts = List(
            Unidentifiable(concepts(3))
          )
        )
      )
    )

    val work = createIdentifiedWorkWith(
      subjects = subjects
    )

    val displayWork = DisplayWorkV1(work)
    val expectedSubjects = concepts
      .map { c: AbstractConcept =>
        DisplayConceptV1(c.label)
      }
    displayWork.subjects shouldBe expectedSubjects
  }

  it("errors if you try to convert a work with non-empty production field") {
    val work = createIdentifiedWorkWith(
      production = createProductionEventList()
    )

    val caught = intercept[IllegalArgumentException] {
      DisplayWorkV1(work)
    }

    caught.getMessage shouldBe s"IdentifiedWork ${work.canonicalId} has production fields set, cannot be converted to a V1 DisplayWork"
  }

  describe("uses the WorksIncludes.identifiers include") {
    val work = createIdentifiedWorkWith(
      itemsV1 = createIdentifiedItems(count = 1)
    )

    describe("omits identifiers if WorksIncludes.identifiers is false") {
      val displayWork = DisplayWorkV1(work, includes = V1WorksIncludes())

      it("the top-level Work") {
        displayWork.identifiers shouldBe None
      }

      it("items") {
        val displayWork =
          DisplayWorkV1(work, includes = V1WorksIncludes(items = true))
        val item: DisplayItemV1 = displayWork.items.get.head
        item.identifiers shouldBe None
      }
    }

    describe("includes identifiers if WorksIncludes.identifiers is true") {
      val displayWork =
        DisplayWorkV1(work, includes = V1WorksIncludes(identifiers = true))

      it("on the top-level Work") {
        displayWork.identifiers shouldBe Some(
          List(DisplayIdentifierV1(work.sourceIdentifier)))
      }

      it("items") {
        val displayWork =
          DisplayWorkV1(
            work,
            includes = V1WorksIncludes(identifiers = true, items = true))
        val item: DisplayItemV1 = displayWork.items.get.head
        item.identifiers shouldBe Some(
          List(DisplayIdentifierV1(work.itemsV1.head.sourceIdentifier)))
      }
    }
  }
}
