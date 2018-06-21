package uk.ac.wellcome.display.models.v2

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.display.models._
import uk.ac.wellcome.models.work.internal._

class DisplayWorkV2Test extends FunSpec with Matchers {

  it("correctly parses a Work without any items") {
    val work = IdentifiedWork(
      title = Some("An irritating imp is immune from items"),
      sourceIdentifier = sourceIdentifier,
      version = 1,
      canonicalId = "abcdef12"
    )

    val displayWork = DisplayWorkV2(
      work = work,
      includes = WorksIncludes(items = true)
    )
    displayWork.items shouldBe Some(List())
  }

  it("correctly parses items on a work") {
    val item = IdentifiedItem(
      canonicalId = "c3a599u5",
      sourceIdentifier = sourceIdentifier,
      locations = List()
    )
    val work = IdentifiedWork(
      title = Some("Inside an irate igloo"),
      sourceIdentifier = sourceIdentifier,
      version = 1,
      canonicalId = "b4heraz7",
      items = List(item)
    )

    val displayWork = DisplayWorkV2(
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

  it("correctly parses a work without any extra identifiers") {
    val work = IdentifiedWork(
      title = Some("An irascible iguana invites impudence"),
      sourceIdentifier = sourceIdentifier,
      version = 1,
      canonicalId = "xtsx8hwk")

    val displayWork = DisplayWorkV2(
      work = work,
      includes = WorksIncludes(identifiers = true)
    )
    displayWork.identifiers shouldBe Some(List(DisplayIdentifierV2(sourceIdentifier)))
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

    val displayWork = DisplayWorkV2(work)
    displayWork.physicalDescription shouldBe Some(physicalDescription)
  }

  it("gets the workType from a Work") {
    val workType = WorkType(
      id = "id",
      label = "Proud pooch pavement plops"
    )

    val expectedDisplayWorkV2 = DisplayWorkType(
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

    val displayWork = DisplayWorkV2(work)
    displayWork.workType shouldBe Some(expectedDisplayWorkV2)
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

    val displayWork = DisplayWorkV2(work)
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

    val displayWork = DisplayWorkV2(work)
    val displayLanguage = displayWork.language.get
    displayLanguage.id shouldBe language.id
    displayLanguage.label shouldBe language.label
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
      DisplayWorkV2(work)
    }

    caught.getMessage shouldBe s"IdentifiedWork ${work.canonicalId} has visible=false, cannot be converted to DisplayWork"
  }

  it("extracts contributors from a Work") {
    val work = IdentifiedWork(
      canonicalId = "vc6zww4f",
      title = Some("Vicarious victory for violet vampires"),
      sourceIdentifier = sourceIdentifier,
      contributors = List(
        Contributor(
          agent = Identified(
            Person(label = "Vlad the Vanquished"),
            canonicalId = "vs7jd5dx",
            sourceIdentifier = SourceIdentifier(
              identifierType = IdentifierType("lc-names"),
              ontologyType = "Person",
              value = "v1"
            )
          )
        ),
        Contributor(
          agent = Unidentifiable(
            Organisation(label = "Transylvania Terrors")
          ),
          roles = List(
            ContributionRole(label = "Background location")
          )
        )
      ),
      version = 1
    )

    val displayWork =
      DisplayWorkV2(work, includes = WorksIncludes(identifiers = true))

    displayWork.contributors shouldBe List(
      DisplayContributor(
        agent = DisplayPersonV2(
          id = Some("vs7jd5dx"),
          label = "Vlad the Vanquished",
          identifiers = Some(
            List(
              DisplayIdentifierV2(
                SourceIdentifier(
                  identifierType = IdentifierType("lc-names"),
                  ontologyType = "Person",
                  value = "v1"
                )
              )
            ))
        ),
        roles = List()
      ),
      DisplayContributor(
        agent = DisplayOrganisationV2(
          id = None,
          label = "Transylvania Terrors",
          identifiers = None
        ),
        roles = List(DisplayContributionRole(label = "Background location"))
      )
    )
  }

  it("extracts production events from a work") {
    val productionEvent = ProductionEvent(
      places = List(Place("London")),
      agents = List(Unidentifiable(Agent("Macmillan"))),
      dates = List(Period("2005")),
      function = Some(Concept("Manufacture"))
    )

    val work = IdentifiedWork(
      canonicalId = "ecgxstzd",
      title = Some("Events entailing the exegesis of empires"),
      sourceIdentifier = sourceIdentifier,
      version = 1,
      production = List(productionEvent)
    )

    val displayWork = DisplayWorkV2(work, includes = AllWorksIncludes())
    displayWork.production shouldBe List(
      DisplayProductionEvent(productionEvent, includesIdentifiers = false))
  }

  describe("correctly uses the WorksIncludes.identifiers include") {
    val contributorAgentSourceIdentifier = SourceIdentifier(
      identifierType = IdentifierType("lc-names"),
      value = "lcnames/007",
      ontologyType = "Agent"
    )

    val contributorPersonSourceIdentifier = SourceIdentifier(
      identifierType = IdentifierType("lc-names"),
      value = "lcnames/bla",
      ontologyType = "Agent"
    )

    val contributorOrganisationSourceIdentifier = SourceIdentifier(
      identifierType = IdentifierType("lc-names"),
      value = "lcnames/bus",
      ontologyType = "Agent"
    )

    val itemSourceIdentifier = SourceIdentifier(
      identifierType = IdentifierType("miro-image-number"),
      value = "miro/b0001",
      ontologyType = "Item"
    )

    val conceptSourceIdentifier = SourceIdentifier(
      identifierType = IdentifierType("lc-subjects"),
      value = "lcsh/bonds",
      ontologyType = "Concept"
    )

    val periodSourceIdentifier = SourceIdentifier(
      identifierType = IdentifierType("lc-subjects"),
      value = "lcsh/before",
      ontologyType = "Concept"
    )

    val placeSourceIdentifier = SourceIdentifier(
      identifierType = IdentifierType("lc-subjects"),
      value = "lcsh/bul",
      ontologyType = "Concept"
    )

    val work = IdentifiedWork(
      canonicalId = "bmzwdx3t",
      title = Some("Bizarre bees bounce below a basketball"),
      sourceIdentifier = sourceIdentifier,
      contributors = List(
        Contributor(
          agent = Identified(
            Agent(label = "Bond"),
            canonicalId = "bcwth7yg",
            sourceIdentifier = contributorAgentSourceIdentifier
          ),
          roles = List()
        ),
        Contributor(
          agent = Identified(
            Organisation(label = "Big Business"),
            canonicalId = "bsf3kfwm",
            sourceIdentifier = contributorOrganisationSourceIdentifier
          ),
          roles = List()
        ),
        Contributor(
          agent = Identified(
            Person(label = "Blue Blaise"),
            canonicalId = "b5szcu3c",
            sourceIdentifier = contributorPersonSourceIdentifier
          ),
          roles = List()
        )
      ),
      items = List(
        IdentifiedItem(
          canonicalId = "bksy8rkc",
          sourceIdentifier = itemSourceIdentifier
        )
      ),
      subjects = List(
        Subject(
          label = "Beryllium-Boron Bonding",
          concepts = List(
            Identified(
              Concept("Bonding"),
              canonicalId = "b5qsqkyh",
              sourceIdentifier = conceptSourceIdentifier
            ),
            Identified(
              Period("Before"),
              canonicalId = "bwn894hk",
              sourceIdentifier = periodSourceIdentifier
            ),
            Identified(
              Place("Bulgaria"),
              canonicalId = "bf42vqst",
              sourceIdentifier = placeSourceIdentifier
            )
          )
        )
      ),
      genres = List(
        Genre(
          label = "Black, Brown and Blue",
          concepts = List(
            Identified(
              Concept("Colours"),
              canonicalId = "chzwu4ea",
              sourceIdentifier = conceptSourceIdentifier
            )
          )
        )
      ),
      version = 1
    )

    describe("omits identifiers if WorksIncludes.identifiers is false") {
      val displayWork = DisplayWorkV2(work, includes = WorksIncludes())

      it("the top-level Work") {
        displayWork.identifiers shouldBe None
      }

      it("contributors") {
        val agents: List[DisplayAbstractAgentV2] =
          displayWork.contributors.map { _.agent }
        agents.map { _.identifiers } shouldBe List(None, None, None)
      }

      it("items") {
        val displayWork =
          DisplayWorkV2(work, includes = WorksIncludes(items = true))
        val item: DisplayItemV2 = displayWork.items.get.head
        item.identifiers shouldBe None
      }

      it("subjects") {
        val concepts = displayWork.subjects.head.concepts
        concepts.map { _.identifiers } shouldBe List(None, None, None)
      }

      it("genres") {
        displayWork.genres.head.concepts.head.identifiers shouldBe None
      }
    }

    describe("includes identifiers if WorksIncludes.identifiers is true") {
      val displayWork =
        DisplayWorkV2(work, includes = WorksIncludes(identifiers = true))

      it("on the top-level Work") {
        displayWork.identifiers shouldBe Some(
          List(DisplayIdentifierV2(sourceIdentifier)))
      }

      it("contributors") {
        // This is moderately verbose, but the Scala compiler got confused when
        // I tried to combine the three map() calls into one.
        val expectedIdentifiers = List(
          contributorAgentSourceIdentifier,
          contributorOrganisationSourceIdentifier,
          contributorPersonSourceIdentifier
        ).map { DisplayIdentifierV2(_) }
          .map { List(_) }
          .map { Some(_) }

        val agents: List[DisplayAbstractAgentV2] =
          displayWork.contributors.map { _.agent }
        agents.map { _.identifiers } shouldBe expectedIdentifiers
      }

      it("items") {
        val displayWork = DisplayWorkV2(
          work,
          includes = WorksIncludes(identifiers = true, items = true))
        val item: DisplayItemV2 = displayWork.items.get.head
        item.identifiers shouldBe Some(
          List(DisplayIdentifierV2(itemSourceIdentifier)))
      }

      it("subjects") {
        val expectedIdentifiers = List(
          conceptSourceIdentifier,
          periodSourceIdentifier,
          placeSourceIdentifier
        ).map { DisplayIdentifierV2(_) }
          .map { List(_) }
          .map { Some(_) }

        val concepts = displayWork.subjects.head.concepts
        concepts.map { _.identifiers } shouldBe expectedIdentifiers
      }

      it("genres") {
        displayWork.genres.head.concepts.head.identifiers shouldBe Some(
          List(DisplayIdentifierV2(conceptSourceIdentifier)))
      }
    }
  }
}
