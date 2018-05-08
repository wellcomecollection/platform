package uk.ac.wellcome.display.models.v1

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.display.models._
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
    identifierScheme = IdentifierSchemes.sierraSystemNumber,
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

  describe("publishers") {
    it("parses a work without any publishers") {
      val work = IdentifiedWork(
        title = Some("A goading giraffe is galling"),
        sourceIdentifier = sourceIdentifier,
        version = 1,
        identifiers = Nil,
        canonicalId = "gsfmhw7v",
        publishers = List()
      )

      val displayWork = DisplayWorkV1(work)
      displayWork.publishers shouldBe List()
    }

    it("parses a work with agent publishers") {
      val work = IdentifiedWork(
        title = Some("A hammerhead hamster is harrowing"),
        sourceIdentifier = sourceIdentifier,
        version = 1,
        identifiers = Nil,
        canonicalId = "hz2hrba9",
        publishers = List(
          Unidentifiable(Agent("Henry Hare")),
          Unidentifiable(Agent("Harriet Heron"))
        )
      )

      val displayWork = DisplayWorkV1(work)
      displayWork.publishers shouldBe List(
        DisplayAgent(
          id = None,
          identifiers = None,
          label = "Henry Hare",
          ontologyType = "Agent"),
        DisplayAgent(
          id = None,
          identifiers = None,
          label = "Harriet Heron",
          ontologyType = "Agent")
      )
    }

    it("parses a work with agents and organisations as publishers") {
      val work = IdentifiedWork(
        title = Some("Jumping over jackals in Japan"),
        sourceIdentifier = sourceIdentifier,
        version = 1,
        identifiers = Nil,
        canonicalId = "j7tw9jv3",
        publishers = List(
          Unidentifiable(Agent("Janet Jackson")),
          Unidentifiable(Organisation("Juniper Journals"))
        )
      )

      val displayWork = DisplayWorkV1(work)
      displayWork.publishers shouldBe List(
        DisplayAgent(id = None, identifiers = None, label = "Janet Jackson"),
        DisplayOrganisation(
          id = None,
          identifiers = None,
          label = "Juniper Journals")
      )
    }
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
      DisplayPerson(
        id = None,
        identifiers = None,
        label = "Esmerelda Weatherwax",
        prefix = Some("Witch")),
      DisplayOrganisation(
        id = None,
        identifiers = None,
        label = "Juniper Journals")
    )
  }

  it(
    "extracts creators from a Work with a mixture of identified/Unidentifiable Contributors") {
    val canonicalId = "abcdefgh"
    val sourceIdentifier = SourceIdentifier(
      IdentifierSchemes.libraryOfCongressNames,
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

    val displayWork = DisplayWorkV1(work)
    displayWork.creators shouldBe List(
      DisplayPerson(
        id = None,
        identifiers = None,
        label = "Esmerelda Weatherwax",
        prefix = Some("Witch")),
      DisplayOrganisation(
        id = Some(canonicalId),
        identifiers = Some(
          List(
            DisplayIdentifier(
              IdentifierSchemes.libraryOfCongressNames.toString,
              sourceIdentifier.value))),
        label = "Juniper Journals"
      )
    )
  }

  it("gets the publicationDate from a Work") {
    val work = IdentifiedWork(
      title = Some("Calling a cacophany of cats to consume carrots"),
      canonicalId = "c4kauupf",
      sourceIdentifier = sourceIdentifier,
      publicationDate = Some(Period("c1900")),
      version = 1
    )

    val displayWork = DisplayWorkV1(work)
    displayWork.publicationDate shouldBe Some(DisplayPeriodV1("c1900"))
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
    val genres = List[Genre[Displayable[AbstractConcept]]](
      Genre(
        label = "a genre created by DisplayWorkV1Test",
        concepts = List(
          Unidentifiable(Concept("a genre concept")),
          Unidentifiable(Place("a genre place")),
          Unidentifiable(Period("a genre period"))
        )
      ),
      Genre(
        label = "a second genre created for DisplayWorkV1Test",
        concepts = List(
          Unidentifiable(Concept("a second generic concept"))
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
    val expectedGenres = genres
      .map { _.concepts }
      .flatten
      .map { case Unidentifiable(concept: Concept) =>
        DisplayConceptV1(label = concept.label)
      }
    displayWork.genres shouldBe expectedGenres
  }

  it("treats subjects as a list of Concepts") {
    val subjects = List[Subject[Displayable[AbstractConcept]]](
      Subject(
        label = "a subject created by DisplayWorkV1Test",
        concepts = List(
          Unidentifiable(Concept("a subject concept")),
          Unidentifiable(Place("a subject place")),
          Unidentifiable(Period("a subject period"))
        )
      ),
      Subject(
        label = "a second subject created for DisplayWorkV1Test",
        concepts = List(
          Unidentifiable(Concept("a second generic concept"))
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
    val expectedSubjects = subjects
      .map { _.concepts }
      .flatten
      .map { case Unidentifiable(concept: Concept) =>
        DisplayConceptV1(label = concept.label)
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

    val caught = intercept[RuntimeException] {
      DisplayWorkV1(work)
    }

    caught.getMessage shouldBe s"IdentifiedWork ${work.canonicalId} has visible=false, cannot be converted to DisplayWork"
  }
}
