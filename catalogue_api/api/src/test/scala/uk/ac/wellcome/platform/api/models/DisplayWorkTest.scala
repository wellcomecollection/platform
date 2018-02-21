package uk.ac.wellcome.platform.api.models

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models._

class DisplayWorkTest extends FunSpec with Matchers {

  it("correctly parses a Work without any items") {
    val work = Work(
      title = Some("An irritating imp is immune from items"),
      sourceIdentifier = sourceIdentifier,
      version = 1,
      identifiers = List(sourceIdentifier),
      canonicalId = Some("abcdef12")
    )

    val displayWork = DisplayWork(
      work = work,
      includes = WorksIncludes(items = true)
    )
    displayWork.items shouldBe Some(List())
  }

  it("correctly parses items on a work") {
    val item = Item(
      canonicalId = Some("c3a599u5"),
      sourceIdentifier = sourceIdentifier,
      identifiers = List(sourceIdentifier),
      locations = List()
    )
    val work = Work(
      title = Some("Inside an irate igloo"),
      sourceIdentifier = sourceIdentifier,
      version = 1,
      identifiers = List(sourceIdentifier),
      canonicalId = Some("b4heraz7"),
      items = List(item)
    )

    val displayWork = DisplayWork(
      work = work,
      includes = WorksIncludes(items = true)
    )
    val displayItem = displayWork.items.get.head
    displayItem.id shouldBe item.canonicalId.get
  }

  val sourceIdentifier = SourceIdentifier(
    identifierScheme = IdentifierSchemes.sierraSystemNumber,
    value = "b1234567"
  )

  it("correctly parses a work without any identifiers") {
    val work = Work(title = Some("An irascible iguana invites impudence"),
                    sourceIdentifier = sourceIdentifier,
                    version = 1,
                    identifiers = Nil,
                    canonicalId = Some("xtsx8hwk"))

    val displayWork = DisplayWork(
      work = work,
      includes = WorksIncludes(identifiers = true)
    )
    displayWork.identifiers shouldBe Some(List())
  }

  describe("publishers") {
    it("parses a work without any publishers") {
      val work = Work(
        title = Some("A goading giraffe is galling"),
        sourceIdentifier = sourceIdentifier,
        version = 1,
        identifiers = Nil,
        canonicalId = Some("gsfmhw7v"),
        publishers = List()
      )

      val displayWork = DisplayWork(work)
      displayWork.publishers shouldBe List()
    }

    it("parses a work with agent publishers") {
      val work = Work(
        title = Some("A hammerhead hamster is harrowing"),
        sourceIdentifier = sourceIdentifier,
        version = 1,
        identifiers = Nil,
        canonicalId = Some("hz2hrba9"),
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
      val work = Work(
        title = Some("Jumping over jackals in Japan"),
        sourceIdentifier = sourceIdentifier,
        version = 1,
        identifiers = Nil,
        canonicalId = Some("j7tw9jv3"),
        publishers = List(
          Agent("Janet Jackson"),
          Organisation("Juniper Journals")
        )
      )

      val displayWork = DisplayWork(work)
      displayWork.publishers shouldBe List(
        new DisplayAgent(label = "Janet Jackson", ontologyType = "Agent"),
        new DisplayAgent(label = "Juniper Journals",
                         ontologyType = "Organisation")
      )
    }
  }
}
