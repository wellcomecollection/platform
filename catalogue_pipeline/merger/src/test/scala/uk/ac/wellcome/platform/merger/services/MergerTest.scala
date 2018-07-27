package uk.ac.wellcome.platform.merger.services

import org.scalatest.FunSpec
import uk.ac.wellcome.models.work.internal._
import uk.ac.wellcome.platform.merger.MergerTestUtils
import uk.ac.wellcome.platform.merger.fixtures.MergerFixtures

class MergerTest extends FunSpec with MergerTestUtils with MergerFixtures {

  private val merger = new Merger()

  it("does not merge a single physical work") {
    assertDoesNotMerge(List(createPhysicalWork))
  }

  it("does not merge a single digital work") {
    assertDoesNotMerge(List(createDigitalWork))
  }

  it("does not merge multiple physical works") {
    assertDoesNotMerge(List.fill(3)(createPhysicalWork))
  }

  it("does not merge multiple digital works") {
    assertDoesNotMerge(List.fill(3)(createDigitalWork))
  }

  it("does not merge multiple physical works with a single digital work") {
    assertDoesNotMerge(List.fill(3)(createPhysicalWork) :+ createDigitalWork)
  }

  it("does not merge a single physical work with multiple digital works") {
    assertDoesNotMerge(
      List(createPhysicalWork) ++ List.fill(3)(createDigitalWork))
  }

  it("merges a physical and digital work") {
    val physicalWork = createPhysicalWork
    val digitalWork = createDigitalWork

    val actualMergedWorks = merger.merge(List(physicalWork, digitalWork))

    actualMergedWorks.size shouldBe 2

    val actualMergedWork = actualMergedWorks.head

    val expectedItems = List(
      physicalWork.items.head.copy(agent = physicalWork.items.head.agent.copy(
        locations = physicalWork.items.head.agent.locations ++ digitalWork.items.head.agent.locations)))
    actualMergedWork shouldBe physicalWork.copy(
      otherIdentifiers = physicalWork.otherIdentifiers ++ digitalWork.identifiers,
      items = expectedItems)

    actualMergedWorks.last shouldBe UnidentifiedRedirectedWork(
      sourceIdentifier = digitalWork.sourceIdentifier,
      version = digitalWork.version,
      redirect = IdentifiableRedirect(physicalWork.sourceIdentifier))
  }

  it("does not merge a physical work having multiple items with a digital work") {
    val works = List(
      createPhysicalWork.copy(items = List(
        createIdentifiableItemWith(locations = List(createPhysicalLocation)),
        createIdentifiableItemWith(locations = List(createPhysicalLocation)))),
      createDigitalWork
    )

    assertDoesNotMerge(works)
  }

  it("does not merge a physical work with a digital work having multiple items") {
    val works = List(
      createPhysicalWork,
      createDigitalWork.copy(
        items = List(
          createIdentifiableItemWith(locations = List(createDigitalLocation)),
          createIdentifiableItemWith(locations = List(createDigitalLocation))))
    )

    assertDoesNotMerge(works)
  }

  private def createDigitalWork = {
    createUnidentifiedWorkWith(
      sourceIdentifier =
        createSourceIdentifierWith(identifierType = "miro-image-number"),
      otherIdentifiers = List(
        createSourceIdentifierWith(identifierType = "miro-library-reference")),
      workType = Some(WorkType("v", "E-books")),
      items = List(
        createIdentifiableItemWith(locations = List(createDigitalLocation)))
    )
  }

  private def createPhysicalWork = {
    createUnidentifiedWorkWith(
      sourceIdentifier =
        createSourceIdentifierWith(identifierType = "sierra-system-number"),
      otherIdentifiers =
        List(createSourceIdentifierWith(identifierType = "sierra-identifier")),
      items = List(
        createIdentifiableItemWith(locations = List(createPhysicalLocation)))
    )
  }

  private def assertDoesNotMerge(works: List[UnidentifiedWork]) = {
    merger.merge(works) should contain theSameElementsAs works
  }
}
