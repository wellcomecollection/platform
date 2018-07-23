package uk.ac.wellcome.platform.merger.services

import org.scalatest.FunSpec
import uk.ac.wellcome.models.work.internal._
import uk.ac.wellcome.platform.merger.MergerTestUtils
import uk.ac.wellcome.platform.merger.fixtures.MergerFixtures

class MergerTest extends FunSpec with MergerTestUtils with MergerFixtures {

  private val merger = new Merger()

  it("returns a single work") {
    val work = createUnidentifiedWork

    merger.merge(List(work)) should contain only work
  }

  it("only physical works are unchanged") {
    val works = createUnidentifiedWorks(3)

    merger.merge(works) should contain theSameElementsAs works
  }

  it("merges a physical and digital work that reference each other") {
    val physicalWork = createUnidentifiedWorkWith(
      items = List(
        createIdentifiableItemWith(locations = List(createPhysicalLocation)))
    )

    val digitalWork = createUnidentifiedWorkWith(
      workType = Some(WorkType("v", "E-books")),
      items = List(
        createIdentifiableItemWith(locations = List(createDigitalLocation)))
    )

    val expectedMergedWork =
      physicalWork.copy(items = physicalWork.items ++ digitalWork.items)

    val expectedRedirectedWork = UnidentifiedRedirectedWork(
      sourceIdentifier = digitalWork.sourceIdentifier,
      version = digitalWork.version,
      redirect = IdentifiableRedirect(expectedMergedWork.sourceIdentifier))

    merger.merge(List(physicalWork, digitalWork)) should
      contain theSameElementsAs List(expectedMergedWork, expectedRedirectedWork)
  }
}
