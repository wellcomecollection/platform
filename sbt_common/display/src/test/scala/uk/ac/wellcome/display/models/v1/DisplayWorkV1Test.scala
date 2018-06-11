package uk.ac.wellcome.display.models.v1

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.display.models._
import uk.ac.wellcome.exceptions.GracefulFailureException
import uk.ac.wellcome.models.work.internal._

class DisplayWorkV1Test extends FunSpec with Matchers {

  it("correctly parses a Work without any items") {
    val work = IdentifiedWork(
      title = Some("An irritating imp is immune from items"),
      sourceIdentifier = sourceIdentifier,
      version = 1,
      identifiers = List(sourceIdentifier),
      canonicalId = "abcdef12"
    )

    val displayWork = DisplayWorkV1(
      work = work,
      includes = WorksIncludes(items = true)
    )
    displayWork.items shouldBe Some(List())
  }

  it("correctly parses items on a work") {
    val item = IdentifiedItem(
      canonicalId = "c3a599u5",
      sourceIdentifier = sourceIdentifier,
      identifiers = List(sourceIdentifier),
      locations = List()
    )
    val work = IdentifiedWork(
      title = Some("Inside an irate igloo"),
      sourceIdentifier = sourceIdentifier,
      version = 1,
      identifiers = List(sourceIdentifier),
      canonicalId = "b4heraz7",
      items = List(item)
    )

    val displayWork = DisplayWorkV1(
      work = work,
      includes = WorksIncludes(items = true)
    )
    val displayItem = displayWork.items.get.head
    displayItem.id shouldBe item.canonicalId
  }

  val sourceIdentifier = SourceIdentifier(
    identifierType = IdentifierType("sierra-system-number"),
    ontologyType = "Work",
    value = "b1234567"
  )

  it("correctly parses a work without any identifiers") {
    val work = IdentifiedWork(
      title = Some("An irascible iguana invites impudence"),
      sourceIdentifier = sourceIdentifier,
      version = 1,
      identifiers = Nil,
      canonicalId = "xtsx8hwk")

    val displayWork = DisplayWorkV1(
      work = work,
      includes = WorksIncludes(identifiers = true)
    )
    displayWork.identifiers shouldBe Some(List())
  }

  it("extracts creators from a Work with Unidentifiable Contributors") {
    val work = IdentifiedWork(
      title = Some("Jumping over jackals in Japan"),
      sourceIdentifier = sourceIdentifier,
      version = 1,
      identifiers = Nil,
      canonicalId = "j7tw9jv3",
      contributors = List(
        Contributor(
          agent = Unidentifiable(
            Person(label = "Esmerelda Weatherwax", prefix = Some("Witch"))
          )
        ),
        Contributor(
          agent = Unidentifiable(
            Organisation("Juniper Journals")
          )
        )
      )
    )

    val displayWork = DisplayWorkV1(work)
    displayWork.creators shouldBe List(
      DisplayPersonV1(
        id = None,
        identifiers = None,
        label = "Esmerelda Weatherwax",
        prefix = Some("Witch")),
      DisplayOrganisationV1(
        id = None,
        identifiers = None,
        label = "Juniper Journals")
    )
  }

  it(
    "extracts creators from a Work with a mixture of identified/Unidentifiable Contributors") {
    val canonicalId = "abcdefgh"
    val sourceIdentifier = SourceIdentifier(
      identifierType = IdentifierType("lc-names"),
      "Organisation",
      "EW")
    val work = IdentifiedWork(
      title = Some("Jumping over jackals in Japan"),
      sourceIdentifier = sourceIdentifier,
      version = 1,
      identifiers = Nil,
      canonicalId = "j7tw9jv3",
      contributors = List(
        Contributor(
          agent = Unidentifiable(
            Person(
              label = "Esmerelda Weatherwax",
              prefix = Some("Witch")
            ))
        ),
        Contributor(
          agent = Identified(
            Organisation(label = "Juniper Journals"),
            identifiers = List(sourceIdentifier),
            canonicalId = canonicalId
          )
        )
      )
    )

    val displayWork =
      DisplayWorkV1(work, includes = WorksIncludes(identifiers = true))
    displayWork.creators shouldBe List(
      DisplayPersonV1(
        id = None,
        identifiers = None,
        label = "Esmerelda Weatherwax",
        prefix = Some("Witch")),
      DisplayOrganisationV1(
        id = Some(canonicalId),
        identifiers = Some(
          List(
            DisplayIdentifierV1(
              identifierScheme = IdentifierType("lc-names").id,
              sourceIdentifier.value))),
        label = "Juniper Journals"
      )
    )
  }

  it("gets the physicalDescription from a Work") {
    val physicalDescription = "A magnificent mural of magpies"

    val work = IdentifiedWork(
      title = Some("Moving a mighty mouse to Madagascar"),
      canonicalId = "mtc2wvrg",
      sourceIdentifier = sourceIdentifier,
      physicalDescription = Some(physicalDescription),
      version = 1
    )

    val displayWork = DisplayWorkV1(work)
    displayWork.physicalDescription shouldBe Some(physicalDescription)
  }

  it("gets the workType from a Work") {
    val workType = WorkType(
      id = "id",
      label = "Proud pooch pavement plops"
    )

    val expectedDisplayWorkV1 = DisplayWorkType(
      id = workType.id,
      label = workType.label
    )

    val work = IdentifiedWork(
      title = Some("Moving a mighty mouse to Madagascar"),
      canonicalId = "mtc2wvrg",
      sourceIdentifier = sourceIdentifier,
      workType = Some(workType),
      version = 1
    )

    val displayWork = DisplayWorkV1(work)
    displayWork.workType shouldBe Some(expectedDisplayWorkV1)
  }

  it("gets the extent from a Work") {
    val extent = "Bound in boxes of bark"

    val work = IdentifiedWork(
      title = Some("Brilliant beeches in Bucharest"),
      canonicalId = "bmnppscn",
      sourceIdentifier = sourceIdentifier,
      extent = Some(extent),
      version = 1
    )

    val displayWork = DisplayWorkV1(work)
    displayWork.extent shouldBe Some(extent)
  }

  it("gets the language from a Work") {
    val language = Language(
      id = "bsl",
      label = "British Sign Language"
    )

    val work = IdentifiedWork(
      title = Some("A largesse of leaping Libyan lions"),
      canonicalId = "lfk6nkje",
      sourceIdentifier = sourceIdentifier,
      language = Some(language),
      version = 1
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

    val work = IdentifiedWork(
      title = Some("A work with genres"),
      canonicalId = "b225839r",
      sourceIdentifier = sourceIdentifier,
      genres = genres,
      version = 1
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

    val subjects = List[Subject[Displayable[AbstractConcept]]](
      Subject(
        label = "a subject created by DisplayWorkV1Test",
        concepts = List(
          Unidentifiable(concepts(0)),
          Unidentifiable(concepts(1)),
          Unidentifiable(concepts(2))
        )
      ),
      Subject(
        label = "a second subject created for DisplayWorkV1Test",
        concepts = List(
          Unidentifiable(concepts(3))
        )
      )
    )

    val work = IdentifiedWork(
      title = Some("A work with subjects"),
      canonicalId = "nznaj8p5",
      sourceIdentifier = sourceIdentifier,
      subjects = subjects,
      version = 1
    )

    val displayWork = DisplayWorkV1(work)
    val expectedSubjects = concepts
      .map { c: AbstractConcept =>
        DisplayConceptV1(c.label)
      }
    displayWork.subjects shouldBe expectedSubjects
  }

  it("gives a helpful error if you try to convert a work with visible=False") {
    val work = IdentifiedWork(
      canonicalId = "xpudrscx",
      title = Some("Invisible igloos improve iguanas"),
      sourceIdentifier = sourceIdentifier,
      visible = false,
      version = 1
    )

    val caught = intercept[GracefulFailureException] {
      DisplayWorkV1(work)
    }

    caught.getMessage shouldBe s"IdentifiedWork ${work.canonicalId} has visible=false, cannot be converted to DisplayWork"
  }

  it("errors if you try to convert a work with non-empty production field") {
    val work = IdentifiedWork(
      canonicalId = "pejk5skd",
      title = Some("Perhaps production of pasta is perfunctory?"),
      sourceIdentifier = sourceIdentifier,
      production = List(
        ProductionEvent(
          places = List(),
          agents = List(),
          dates = List(),
          function = Some(Concept("Manufacture"))
        )
      ),
      version = 1
    )

    val caught = intercept[GracefulFailureException] {
      DisplayWorkV1(work)
    }

    caught.getMessage shouldBe s"IdentifiedWork ${work.canonicalId} has production fields set, cannot be converted to a V1 DisplayWork"
  }

  describe("correctly uses the WorksIncludes.identifiers include") {
    val creatorAgentSourceIdentifier = SourceIdentifier(
      identifierType = IdentifierType("lc-names"),
      value = "lcnames/pp",
      ontologyType = "Agent"
    )

    val creatorPersonSourceIdentifier = SourceIdentifier(
      identifierType = IdentifierType("lc-names"),
      value = "lcnames/pri",
      ontologyType = "Agent"
    )

    val creatorOrganisationSourceIdentifier = SourceIdentifier(
      identifierType = IdentifierType("lc-names"),
      value = "lcnames/pri",
      ontologyType = "Agent"
    )

    val itemSourceIdentifier = SourceIdentifier(
      identifierType = IdentifierType("miro-image-number"),
      value = "miro/p0001",
      ontologyType = "Item"
    )

    val work = IdentifiedWork(
      canonicalId = "pt5vupg4",
      title = Some("Pouncing pugs play in pipes"),
      sourceIdentifier = sourceIdentifier,
      identifiers = List(sourceIdentifier),
      contributors = List(
        Contributor(
          agent = Identified(
            Agent(label = "Purple Penelope"),
            canonicalId = "p72ujfbe",
            identifiers = List(creatorAgentSourceIdentifier)
          ),
          roles = List()
        ),
        Contributor(
          agent = Identified(
            Organisation(label = "Pretty Prints"),
            canonicalId = "pqcmakdp",
            identifiers = List(creatorOrganisationSourceIdentifier)
          ),
          roles = List()
        ),
        Contributor(
          agent = Identified(
            Person(label = "Private Paul"),
            canonicalId = "pcynevb6",
            identifiers = List(creatorPersonSourceIdentifier)
          ),
          roles = List()
        )
      ),
      items = List(
        IdentifiedItem(
          canonicalId = "pwaazubr",
          sourceIdentifier = itemSourceIdentifier,
          identifiers = List(itemSourceIdentifier)
        )
      ),
      version = 1
    )

    describe("omits identifiers if WorksIncludes.identifiers is false") {
      val displayWork = DisplayWorkV1(work, includes = WorksIncludes())

      it("the top-level Work") {
        displayWork.identifiers shouldBe None
      }

      it("creators") {
        displayWork.creators.map { _.identifiers } shouldBe List(
          None,
          None,
          None)
      }

      it("items") {
        val displayWork =
          DisplayWorkV1(work, includes = WorksIncludes(items = true))
        val item: DisplayItemV1 = displayWork.items.get.head
        item.identifiers shouldBe None
      }
    }

    describe("includes identifiers if WorksIncludes.identifiers is true") {
      val displayWork =
        DisplayWorkV1(work, includes = WorksIncludes(identifiers = true))

      it("on the top-level Work") {
        displayWork.identifiers shouldBe Some(
          List(DisplayIdentifierV1(sourceIdentifier)))
      }

      it("creators") {
        // This is moderately verbose, but the Scala compiler got confused when
        // I tried to combine the three map() calls into one.
        val expectedIdentifiers = List(
          creatorAgentSourceIdentifier,
          creatorOrganisationSourceIdentifier,
          creatorPersonSourceIdentifier
        ).map { DisplayIdentifierV1(_) }
          .map { List(_) }
          .map { Some(_) }
        displayWork.creators.map { _.identifiers } shouldBe expectedIdentifiers
      }

      it("items") {
        val displayWork = DisplayWorkV1(
          work,
          includes = WorksIncludes(identifiers = true, items = true))
        val item: DisplayItemV1 = displayWork.items.get.head
        item.identifiers shouldBe Some(
          List(DisplayIdentifierV1(itemSourceIdentifier)))
      }
    }
  }
}
