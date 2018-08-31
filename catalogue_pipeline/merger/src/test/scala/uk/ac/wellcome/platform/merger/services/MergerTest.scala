package uk.ac.wellcome.platform.merger.services

import org.scalatest.FunSpec
import uk.ac.wellcome.models.work.internal._
import uk.ac.wellcome.platform.merger.MergerTestUtils
import uk.ac.wellcome.platform.merger.pairwise_transforms.SierraPhysicalDigitalWorkPair

class MergerTest extends FunSpec with MergerTestUtils {

  private val merger = new Merger()

  describe("maybeMergePhysicalDigitalWorkPair") {
    it("merges a physical and digital work") {
      val physicalWork = createPhysicalWork
      val digitalWork = createDigitalWork

      val result = merger.maybeMergePhysicalDigitalWorkPair(
        works = Seq(physicalWork, digitalWork)
      )

      result.size shouldBe 2

      val physicalItem = physicalWork.items.head.asInstanceOf[Identifiable[Item]]
      val digitalItem = digitalWork.items.head

      val expectedMergedWork = physicalWork.copy(
        otherIdentifiers = physicalWork.otherIdentifiers ++ digitalWork.identifiers,
        items = List(
          physicalItem.copy(
            agent = physicalItem.agent.copy(
              locations = physicalItem.agent.locations ++ digitalItem.agent.locations
            )
          )
        )
      )

      val expectedRedirectedWork = UnidentifiedRedirectedWork(
        sourceIdentifier = digitalWork.sourceIdentifier,
        version = digitalWork.version,
        redirect = IdentifiableRedirect(physicalWork.sourceIdentifier)
      )

      result should contain theSameElementsAs List(expectedMergedWork, expectedRedirectedWork)
    }

    it("ignores a single physical work")  {
      val works = Seq(createPhysicalWork)
      merger.maybeMergePhysicalDigitalWorkPair(works) shouldBe works
    }

    it("ignores a single digital work") {
      val works = Seq(createDigitalWork)
      merger.maybeMergePhysicalDigitalWorkPair(works) shouldBe works
    }

    it("ignores multiple physical works") {
      val works = (1 to 3).map { _ => createPhysicalWork }
      merger.maybeMergePhysicalDigitalWorkPair(works) shouldBe works
    }

    it("ignores multiple digital works") {
      val works = (1 to 3).map { _ => createDigitalWork }
      merger.maybeMergePhysicalDigitalWorkPair(works) shouldBe works
    }

    it("ignores multiple physical works with a single digital work") {
      val works = (1 to 3).map { _ => createPhysicalWork } ++ Seq(createDigitalWork)
      merger.maybeMergePhysicalDigitalWorkPair(works) shouldBe works
    }

    it("ignores multiple digital works with a single physical work") {
      val works = (1 to 3).map { _ => createDigitalWork } ++ Seq(createPhysicalWork)
      merger.maybeMergePhysicalDigitalWorkPair(works) shouldBe works
    }

    it("ignores a physical work with multiple items") {
      val works = List(
        createPhysicalWorkWith(
          items = List(createPhysicalItem, createPhysicalItem)
        ),
        createDigitalWork
      )
      merger.maybeMergePhysicalDigitalWorkPair(works) shouldBe works
    }

    it("ignores a digital work with multiple items") {
      val works = List(
        createPhysicalWork,
        createDigitalWorkWith(
          items = List(createDigitalItem, createDigitalItem)
        )
      )
      merger.maybeMergePhysicalDigitalWorkPair(works) shouldBe works
    }
  }

  it("merges a physical and digital work") {
    val physicalWork = createPhysicalWork
    val digitalWork = createDigitalWork

    val result = merger.merge(
      works = Seq(physicalWork, digitalWork)
    )

    result.size shouldBe 2

    val physicalItem = physicalWork.items.head.asInstanceOf[Identifiable[Item]]
    val digitalItem = digitalWork.items.head

    val expectedMergedWork = physicalWork.copy(
      otherIdentifiers = physicalWork.otherIdentifiers ++ digitalWork.identifiers,
      items = List(
        physicalItem.copy(
          agent = physicalItem.agent.copy(
            locations = physicalItem.agent.locations ++ digitalItem.agent.locations
          )
        )
      )
    )

    val expectedRedirectedWork = UnidentifiedRedirectedWork(
      sourceIdentifier = digitalWork.sourceIdentifier,
      version = digitalWork.version,
      redirect = IdentifiableRedirect(physicalWork.sourceIdentifier)
    )

    result should contain theSameElementsAs List(expectedMergedWork, expectedRedirectedWork)
  }

  private def createPhysicalItem: Identifiable[Item] =
    createIdentifiableItemWith(locations = List(createPhysicalLocation))

  private def createDigitalItem: Unidentifiable[Item] =
    createUnidentifiableItemWith(locations = List(createDigitalLocation))
}
