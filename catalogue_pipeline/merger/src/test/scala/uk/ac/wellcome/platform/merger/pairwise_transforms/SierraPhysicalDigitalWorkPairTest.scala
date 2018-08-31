package uk.ac.wellcome.platform.merger.pairwise_transforms

import org.scalatest.FunSpec
import uk.ac.wellcome.models.work.internal.{Identifiable, Item, Unidentifiable, UnidentifiedWork}
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
