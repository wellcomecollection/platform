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

    val expectedMergedWork = physicalWork.copy(
      items = List(
        Identifiable(
          sourceIdentifier =
            physicalWork.items.head.sourceIdentifier,
          agent =
            Item(locations = physicalWork.items.head.agent.locations ++ digitalWork.items.head.agent.locations)
      )))

    val expectedRedirectedWork = UnidentifiedRedirectedWork(
      sourceIdentifier = digitalWork.sourceIdentifier,
      version = digitalWork.version,
      redirect = IdentifiableRedirect(expectedMergedWork.sourceIdentifier))

    merger.merge(List(physicalWork, digitalWork)) should
      contain theSameElementsAs List(expectedMergedWork, expectedRedirectedWork)
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
      sourceIdentifier = createSourceIdentifierWith(identifierType="miro-image-number"),
      workType = Some(WorkType("v", "E-books")),
      items = List(
        createIdentifiableItemWith(locations = List(createDigitalLocation)))
    )
  }

  private def createPhysicalWork = {
    createUnidentifiedWorkWith(
      sourceIdentifier = createSourceIdentifierWith(identifierType="sierra-system-number"),
      items = List(
        createIdentifiableItemWith(locations = List(createPhysicalLocation)))
    )
  }

  private def assertDoesNotMerge(works: List[UnidentifiedWork]) = {
    merger.merge(works) should contain theSameElementsAs works
  }
}
