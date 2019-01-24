package uk.ac.wellcome.display.models.v2

import org.scalatest.prop.PropertyChecks
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.display.models._
import uk.ac.wellcome.models.work.internal._
import org.scalacheck.ScalacheckShapeless._
import uk.ac.wellcome.models.work.generators.{
  ProductionEventGenerators,
  WorksGenerators
}

class DisplayWorkV2Test
    extends FunSpec
    with Matchers
    with ProductionEventGenerators
    with WorksGenerators
    with PropertyChecks {

  it("parses a Work without any items") {
    val work = createIdentifiedWorkWith(
      items = List()
    )

    val displayWork = DisplayWorkV2(
      work = work,
      includes = V2WorksIncludes(items = true)
    )
    displayWork.items shouldBe Some(List())
  }

  it("parses identified items on a work") {
    val items = createIdentifiedItems(count = 1)
    val work = createIdentifiedWorkWith(
      items = items
    )

    val displayWork = DisplayWorkV2(
      work = work,
      includes = V2WorksIncludes(items = true)
    )
    val displayItem = displayWork.items.get.head
    displayItem.id shouldBe Some(items.head.canonicalId)
  }

  it("parses unidentified items on a work") {
    val item = createUnidentifiableItemWith()
    val location = item.agent.locations.head.asInstanceOf[DigitalLocation]
    val work = createIdentifiedWorkWith(
      items = List(item)
    )

    val displayWork = DisplayWorkV2(
      work = work,
      includes = V2WorksIncludes(items = true)
    )

    val displayItem = displayWork.items.get.head
    displayItem shouldBe DisplayItemV2(
      id = None,
      identifiers = None,
      locations = List(
        DisplayDigitalLocationV2(
          DisplayLocationType(location.locationType),
          url = location.url,
          credit = location.credit,
          license = location.license.map { DisplayLicenseV2.apply }))
    )
  }

  it("parses a work without any extra identifiers") {
    val work = createIdentifiedWorkWith(
      otherIdentifiers = List()
    )

    val displayWork = DisplayWorkV2(
      work = work,
      includes = V2WorksIncludes(identifiers = true)
    )
    displayWork.identifiers shouldBe Some(
      List(DisplayIdentifierV2(work.sourceIdentifier)))
  }

  it("gets the physicalDescription from a Work") {
    val physicalDescription = "A magnificent mural of magpies"

    val work = createIdentifiedWorkWith(
      physicalDescription = Some(physicalDescription)
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

    val work = createIdentifiedWorkWith(
      workType = Some(workType)
    )

    val displayWork = DisplayWorkV2(work)
    displayWork.workType shouldBe Some(expectedDisplayWorkV2)
  }

  it("gets the extent from a Work") {
    val extent = "Bound in boxes of bark"

    val work = createIdentifiedWorkWith(
      extent = Some(extent)
    )

    val displayWork = DisplayWorkV2(work)
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

    val displayWork = DisplayWorkV2(work)
    val displayLanguage = displayWork.language.get
    displayLanguage.id shouldBe language.id
    displayLanguage.label shouldBe language.label
  }

  it("extracts contributors from a Work with the contributors include") {
    val canonicalId = createCanonicalId
    val sourceIdentifier = createSourceIdentifierWith(
      ontologyType = "Person"
    )

    val work = createIdentifiedWorkWith(
      contributors = List(
        Contributor(
          agent = Identified(
            Person(label = "Vlad the Vanquished"),
            canonicalId = canonicalId,
            sourceIdentifier = sourceIdentifier
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
      )
    )

    val displayWork =
      DisplayWorkV2(
        work,
        includes = V2WorksIncludes(identifiers = true, contributors = true))

    displayWork.contributors.get shouldBe List(
      DisplayContributor(
        agent = DisplayPersonV2(
          id = Some(canonicalId),
          label = "Vlad the Vanquished",
          identifiers = Some(
            List(DisplayIdentifierV2(sourceIdentifier))
          )
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

  it("extracts production events from a work with the production include") {
    val productionEvent = createProductionEvent

    val work = createIdentifiedWorkWith(
      production = List(productionEvent)
    )

    val displayWork =
      DisplayWorkV2(work, includes = V2WorksIncludes(production = true))
    displayWork.production.get shouldBe List(
      DisplayProductionEvent(productionEvent, includesIdentifiers = false))
  }

  it("does not extract includes set to false") {
    forAll { work: IdentifiedWork =>
      val displayWork =
        DisplayWorkV2(work, includes = V2WorksIncludes())

      displayWork.production shouldNot be(defined)
      displayWork.subjects shouldNot be(defined)
      displayWork.genres shouldNot be(defined)
      displayWork.contributors shouldNot be(defined)
      displayWork.items shouldNot be(defined)
      displayWork.identifiers shouldNot be(defined)
    }
  }

  describe("uses the WorksIncludes.identifiers include") {
    val contributorAgentSourceIdentifier = createSourceIdentifierWith(
      ontologyType = "Agent"
    )

    val contributorPersonSourceIdentifier = createSourceIdentifierWith(
      ontologyType = "Agent"
    )

    val contributorOrganisationSourceIdentifier = createSourceIdentifierWith(
      ontologyType = "Agent"
    )

    val subjectSourceIdentifier = createSourceIdentifierWith(
      ontologyType = "Subject"
    )

    val conceptSourceIdentifier = createSourceIdentifierWith(
      ontologyType = "Concept"
    )

    val periodSourceIdentifier = createSourceIdentifierWith(
      ontologyType = "Concept"
    )

    val placeSourceIdentifier = createSourceIdentifierWith(
      ontologyType = "Concept"
    )

    val work = createIdentifiedWorkWith(
      contributors = List(
        Contributor(
          agent = Identified(
            Agent(label = "Bond"),
            canonicalId = createCanonicalId,
            sourceIdentifier = contributorAgentSourceIdentifier
          ),
          roles = List()
        ),
        Contributor(
          agent = Identified(
            Organisation(label = "Big Business"),
            canonicalId = createCanonicalId,
            sourceIdentifier = contributorOrganisationSourceIdentifier
          ),
          roles = List()
        ),
        Contributor(
          agent = Identified(
            Person(label = "Blue Blaise"),
            canonicalId = createCanonicalId,
            sourceIdentifier = contributorPersonSourceIdentifier
          ),
          roles = List()
        )
      ),
      items = createIdentifiedItems(count = 1),
      subjects = List(
        Identified(
          Subject(
            label = "Beryllium-Boron Bonding",
            concepts = List(
              Identified(
                Concept("Bonding"),
                canonicalId = createCanonicalId,
                sourceIdentifier = conceptSourceIdentifier
              ),
              Identified(
                Period("Before"),
                canonicalId = createCanonicalId,
                sourceIdentifier = periodSourceIdentifier
              ),
              Identified(
                Place("Bulgaria"),
                canonicalId = createCanonicalId,
                sourceIdentifier = placeSourceIdentifier
              )
            )
          ),
          canonicalId = createCanonicalId,
          sourceIdentifier = subjectSourceIdentifier
        )
      ),
      genres = List(
        Genre(
          label = "Black, Brown and Blue",
          concepts = List(
            Identified(
              Concept("Colours"),
              canonicalId = createCanonicalId,
              sourceIdentifier = conceptSourceIdentifier
            )
          )
        )
      )
    )

    describe("omits identifiers if WorksIncludes.identifiers is false") {
      val displayWork = DisplayWorkV2(work, includes = V2WorksIncludes())

      it("the top-level Work") {
        displayWork.identifiers shouldBe None
      }

      it("contributors") {
        val displayWork =
          DisplayWorkV2(work, includes = V2WorksIncludes(contributors = true))
        val agents: List[DisplayAbstractAgentV2] =
          displayWork.contributors.get.map { _.agent }
        agents.map { _.identifiers } shouldBe List(None, None, None)
      }

      it("items") {
        val displayWork =
          DisplayWorkV2(work, includes = V2WorksIncludes(items = true))
        val item: DisplayItemV2 = displayWork.items.get.head
        item.identifiers shouldBe None
      }

      it("subjects") {
        val displayWork =
          DisplayWorkV2(work, includes = V2WorksIncludes(subjects = true))
        val subject = displayWork.subjects.get.head
        subject.identifiers shouldBe None

        val concepts = subject.concepts
        concepts.map { _.identifiers } shouldBe List(None, None, None)
      }

      it("genres") {
        val displayWork =
          DisplayWorkV2(work, includes = V2WorksIncludes(genres = true))
        displayWork.genres.get.head.concepts.head.identifiers shouldBe None
      }
    }

    describe("includes identifiers if WorksIncludes.identifiers is true") {
      val displayWork =
        DisplayWorkV2(work, includes = V2WorksIncludes(identifiers = true))

      it("on the top-level Work") {
        displayWork.identifiers shouldBe Some(
          List(DisplayIdentifierV2(work.sourceIdentifier)))
      }

      it("contributors") {
        val displayWork =
          DisplayWorkV2(
            work,
            includes = V2WorksIncludes(contributors = true, identifiers = true))

        val expectedIdentifiers = List(
          contributorAgentSourceIdentifier,
          contributorOrganisationSourceIdentifier,
          contributorPersonSourceIdentifier
        ).map { identifier =>
          Some(List(DisplayIdentifierV2(identifier)))
        }

        val agents: List[DisplayAbstractAgentV2] =
          displayWork.contributors.get.map { _.agent }
        agents.map { _.identifiers } shouldBe expectedIdentifiers
      }

      it("items") {
        val displayWork = DisplayWorkV2(
          work,
          includes = V2WorksIncludes(identifiers = true, items = true))
        val item: DisplayItemV2 = displayWork.items.get.head
        val identifiedItem = work.items.head.asInstanceOf[Identified[Item]]
        item.identifiers shouldBe Some(
          List(DisplayIdentifierV2(identifiedItem.sourceIdentifier)))
      }

      it("subjects") {
        val displayWork =
          DisplayWorkV2(
            work,
            includes = V2WorksIncludes(identifiers = true, subjects = true))
        val expectedIdentifiers = List(
          conceptSourceIdentifier,
          periodSourceIdentifier,
          placeSourceIdentifier
        ).map { DisplayIdentifierV2(_) }
          .map { List(_) }
          .map { Some(_) }

        val subject = displayWork.subjects.get.head
        subject.identifiers shouldBe Some(
          List(DisplayIdentifierV2(subjectSourceIdentifier)))

        val concepts = subject.concepts
        concepts.map { _.identifiers } shouldBe expectedIdentifiers
      }

      it("genres") {
        val displayWork =
          DisplayWorkV2(
            work,
            includes = V2WorksIncludes(identifiers = true, genres = true))
        displayWork.genres.get.head.concepts.head.identifiers shouldBe Some(
          List(DisplayIdentifierV2(conceptSourceIdentifier)))
      }
    }
  }
}
