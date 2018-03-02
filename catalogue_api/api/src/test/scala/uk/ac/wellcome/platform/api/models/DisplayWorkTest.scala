package uk.ac.wellcome.platform.api.models

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models._

class DisplayWorkTest extends FunSpec with Matchers {

  it("correctly parses a Work without any items") {
    val work = IdentifiedWork(
      title = Some("An irritating imp is immune from items"),
      sourceIdentifier = sourceIdentifier,
      version = 1,
      identifiers = List(sourceIdentifier),
      canonicalId = "abcdef12"
    )

    val displayWork = DisplayWork(
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

    val displayWork = DisplayWork(
      work = work,
      includes = WorksIncludes(items = true)
    )
    val displayItem = displayWork.items.get.head
    displayItem.id shouldBe item.canonicalId
  }

  val sourceIdentifier = SourceIdentifier(
    identifierScheme = IdentifierSchemes.sierraSystemNumber,
    value = "b1234567"
  )

  it("correctly parses a work without any identifiers") {
    val work = IdentifiedWork(
      title = Some("An irascible iguana invites impudence"),
      sourceIdentifier = sourceIdentifier,
      version = 1,
      identifiers = Nil,
      canonicalId = "xtsx8hwk")

    val displayWork = DisplayWork(
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

      val displayWork = DisplayWork(work)
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
          Agent("Henry Hare"),
          Agent("Harriet Heron")
        )
      )

      val displayWork = DisplayWork(work)
      displayWork.publishers shouldBe List(
        new DisplayAgent(label = "Henry Hare", ontologyType = "Agent"),
        new DisplayAgent(label = "Harriet Heron", ontologyType = "Agent")
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
          Agent("Janet Jackson"),
          Organisation("Juniper Journals")
        )
      )

      val displayWork = DisplayWork(work)
      displayWork.publishers shouldBe List(
        new DisplayAgent(label = "Janet Jackson", ontologyType = "Agent"),
        new DisplayAgent(
          label = "Juniper Journals",
          ontologyType = "Organisation")
      )
    }
  }

  it("gets the publicationDate from a Work") {
    val work = IdentifiedWork(
      title = Some("Calling a cacophany of cats to consume carrots"),
      canonicalId = "c4kauupf",
      sourceIdentifier = sourceIdentifier,
      publicationDate = Some(Period("c1900")),
      version = 1
    )

    val displayWork = DisplayWork(work)
    displayWork.publicationDate shouldBe Some(DisplayPeriod("c1900"))
  }

  it("gets the physicalDescription from a Work") {
    val physicalDescription = "A magnificent mural of magpies"

    val Work = IdentifiedWork(
      title = Some("Moving a mighty mouse to Madagascar"),
      canonicalId = "mtc2wvrg",
      sourceIdentifier = sourceIdentifier,
      physicalDescription = Some(physicalDescription),
      version = 1
    )

    val displayWork = DisplayPeriod(work)
    displayWork.physicalDescription shouldBe Some(physicalDescription)
  }
}
