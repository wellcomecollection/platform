package uk.ac.wellcome.platform.merger.pairwise_transforms

import org.scalatest.FunSpec
import uk.ac.wellcome.models.work.internal._
import uk.ac.wellcome.platform.merger.MergerTestUtils

class SierraPhysicalDigitalWorkPairTest extends FunSpec with MergerTestUtils {
  val physicalWorkWithOneItem: UnidentifiedWork = createPhysicalWork
  val digitalWorkWithOneItem: UnidentifiedWork = createDigitalWork

  it("copies the identifiers from the digital work to the physical work") {
    val digitalWork = createUnidentifiedWorkWith(
      otherIdentifiers = List(createSourceIdentifier, createSourceIdentifier),
      items = List(createDigitalItem)
    )

    val result = applyMerge(
      physicalWork = physicalWorkWithOneItem,
      digitalWork = digitalWork
    )

    val newIdentifiers = result.get.mergedWork.identifiers
    newIdentifiers shouldBe physicalWorkWithOneItem.identifiers ++ digitalWork.identifiers
  }

  it("copies the location from the digital work to the physical work") {
    val result = applyMerge(
      physicalWork = physicalWorkWithOneItem,
      digitalWork = digitalWorkWithOneItem
    )

    val actualLocations = result.get.mergedWork.items.head.agent.locations
    val expectedLocations =
      physicalWorkWithOneItem.items.head.agent.locations ++
        digitalWorkWithOneItem.items.head.agent.locations
    actualLocations shouldBe expectedLocations
  }

  it("redirects the digital work to the physical work") {
    val result = applyMerge(
      physicalWork = physicalWorkWithOneItem,
      digitalWork = digitalWorkWithOneItem
    )

    result.get.redirectedWork shouldBe UnidentifiedRedirectedWork(
      sourceIdentifier = digitalWorkWithOneItem.sourceIdentifier,
      version = digitalWorkWithOneItem.version,
      redirect = IdentifiableRedirect(physicalWorkWithOneItem.sourceIdentifier)
    )
  }

  it("uses the physical work as the basis for the merged work") {
    val result = applyMerge(
      physicalWork = physicalWorkWithOneItem,
      digitalWork = digitalWorkWithOneItem
    )

    val expectedLocations =
      physicalWorkWithOneItem.items.head.agent.locations ++
        digitalWorkWithOneItem.items.head.agent.locations

    val expectedItem = physicalWorkWithOneItem.items.head
      .asInstanceOf[Identifiable[Item]]
      .copy(
        agent = physicalWorkWithOneItem.items.head.agent.copy(
          locations = expectedLocations
        )
      )

    val expectedMergedWork = physicalWorkWithOneItem.copy(
      otherIdentifiers =
        physicalWorkWithOneItem.otherIdentifiers ++ digitalWorkWithOneItem.identifiers,
      items = List(expectedItem)
    )

    result.get.mergedWork shouldBe expectedMergedWork
  }

  describe("does not merge if item counts are wrong") {
    it("physical work has 0 items") {
      val physicalWork = createUnidentifiedWorkWith(
        items = List()
      )

      applyMerge(
        physicalWork = physicalWork,
        digitalWork = digitalWorkWithOneItem
      ) shouldBe None
    }

    it("physical work has >1 items") {
      val physicalWork = createUnidentifiedWorkWith(
        items = List(createPhysicalItem, createPhysicalItem)
      )

      applyMerge(
        physicalWork = physicalWork,
        digitalWork = digitalWorkWithOneItem
      ) shouldBe None
    }

    it("digital work has 0 items") {
      val digitalWork = createUnidentifiedWorkWith(
        items = List()
      )

      applyMerge(
        physicalWork = physicalWorkWithOneItem,
        digitalWork = digitalWork
      ) shouldBe None
    }

    it("digital work has >1 items") {
      val digitalWork = createUnidentifiedWorkWith(
        items = List(createDigitalItem, createDigitalItem)
      )

      applyMerge(
        physicalWork = physicalWorkWithOneItem,
        digitalWork = digitalWork
      ) shouldBe None
    }
  }

  private def createPhysicalItem: Identifiable[Item] =
    createIdentifiableItemWith(locations = List(createPhysicalLocation))

  private def createDigitalItem: Unidentifiable[Item] =
    createUnidentifiableItemWith(locations = List(createDigitalLocation))

  private def applyMerge(
    physicalWork: UnidentifiedWork,
    digitalWork: UnidentifiedWork): Option[PairwiseResult] =
    SierraPhysicalDigitalWorkPair.mergeAndRedirectWork(
      physicalWork = physicalWork,
      digitalWork = digitalWork
    )
}
