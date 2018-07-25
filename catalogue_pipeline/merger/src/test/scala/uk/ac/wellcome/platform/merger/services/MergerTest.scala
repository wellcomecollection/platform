package uk.ac.wellcome.platform.merger.services

import org.scalatest.FunSpec
import uk.ac.wellcome.models.work.internal._
import uk.ac.wellcome.platform.merger.MergerTestUtils
import uk.ac.wellcome.platform.merger.fixtures.MergerFixtures

class MergerTest extends FunSpec with MergerTestUtils with MergerFixtures {

  private val merger = new Merger()

  it("returns a single physical work") {
    val work = createPhysicalWork

    merger.merge(List(work)) should contain only work
  }

  it("returns a single digital work") {
    val work = createDigitalWork

    merger.merge(List(work)) should contain only work
  }

  it("merging multiple physical works does not change them") {
    val works = List.fill(3)(createPhysicalWork)

    merger.merge(works) should contain theSameElementsAs works
  }

  it("merging multiple digital works does not change them") {
    val works = List.fill(3)(createDigitalWork)

    merger.merge(works) should contain theSameElementsAs works
  }

  it(
    "merging  more than one physical work with a single digital work does not change them") {
    val works = List.fill(3)(createPhysicalWork) :+ createDigitalWork

    merger.merge(works) should contain theSameElementsAs works
  }

  it(
    "merging a single physical work with multiple digital works does not change them") {
    val works = List(createPhysicalWork) ++ List.fill(3)(createDigitalWork)

    merger.merge(works) should contain theSameElementsAs works
  }

  it("merges a physical and digital work") {
    val physicalWork = createPhysicalWork
    val digitalWork = createDigitalWork

    val expectedMergedWork =
      physicalWork.copy(items = physicalWork.items ++ digitalWork.items)

    val expectedRedirectedWork = UnidentifiedRedirectedWork(
      sourceIdentifier = digitalWork.sourceIdentifier,
      version = digitalWork.version,
      redirect = IdentifiableRedirect(expectedMergedWork.sourceIdentifier))

    merger.merge(List(physicalWork, digitalWork)) should
      contain theSameElementsAs List(expectedMergedWork, expectedRedirectedWork)
  }

  private def createDigitalWork = {
    createUnidentifiedWorkWith(
      workType = Some(WorkType("v", "E-books")),
      items = List(
        createIdentifiableItemWith(locations = List(createDigitalLocation)))
    )
  }

  private def createPhysicalWork = {
    createUnidentifiedWorkWith(
      items = List(
        createIdentifiableItemWith(locations = List(createPhysicalLocation)))
    )
  }
}
