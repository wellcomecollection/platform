package uk.ac.wellcome.platform.api.models

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models._

class DisplayWorkTest extends FunSpec with Matchers {

  it("correctly parses a Work without any items") {
    val work = Work(title = Some("An irritating imp is immune from items"), sourceIdentifier = sourceIdentifier, identifiers = List(sourceIdentifier), canonicalId = Some("abcdef12"))

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
    val work = Work(title = Some("Inside an irate igloo"), sourceIdentifier = sourceIdentifier, identifiers = List(sourceIdentifier), canonicalId = Some("b4heraz7"), items = List(item))

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
    val work = Work(title = Some("An irascible iguana invites impudence"), sourceIdentifier = sourceIdentifier, identifiers = Nil, canonicalId = Some("xtsx8hwk"))

    val displayWork = DisplayWork(
      work = work,
      includes = WorksIncludes(identifiers = true)
    )
    displayWork.identifiers shouldBe Some(List())
  }
}
