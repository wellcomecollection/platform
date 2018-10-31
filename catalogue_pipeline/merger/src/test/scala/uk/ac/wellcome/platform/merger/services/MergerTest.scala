package uk.ac.wellcome.platform.merger.services

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.work.generators.WorksGenerators
import uk.ac.wellcome.models.work.internal._

class MergerTest extends FunSpec with WorksGenerators with Matchers {
  private val sierraPhysicalWork = createSierraPhysicalWork
  private val sierraDigitalWork = createSierraDigitalWork
  private val miroWork = createMiroWork

  private val merger = new Merger()

  it("merges a Sierra physical and Sierra digital work") {
    val result = merger.merge(
      works = Seq(sierraPhysicalWork, sierraDigitalWork)
    )

    result.size shouldBe 2

    val physicalItem =
      sierraPhysicalWork.items.head.asInstanceOf[Identifiable[Item]]
    val digitalItem = sierraDigitalWork.items.head

    val expectedMergedWork = sierraPhysicalWork.copy(
      otherIdentifiers = sierraPhysicalWork.otherIdentifiers ++ sierraDigitalWork.identifiers,
      items = List(
        physicalItem.copy(
          agent = physicalItem.agent.copy(
            locations = physicalItem.agent.locations ++ digitalItem.agent.locations
          )
        )
      )
    )

    val expectedRedirectedWork =
      UnidentifiedRedirectedWork(sierraDigitalWork, sierraPhysicalWork)

    result should contain theSameElementsAs List(
      expectedMergedWork,
      expectedRedirectedWork)
  }

  it("merges a Sierra physical work with a single-page Miro work") {
    val result = merger.merge(
      works = Seq(sierraPhysicalWork, miroWork)
    )

    result.size shouldBe 2

    val sierraItem =
      sierraPhysicalWork.items.head.asInstanceOf[Identifiable[Item]]
    val miroItem = miroWork.items.head

    val expectedMergedWork = sierraPhysicalWork.copy(
      otherIdentifiers = sierraPhysicalWork.otherIdentifiers ++ miroWork.identifiers,
      items = List(
        sierraItem.copy(
          agent = sierraItem.agent.copy(
            locations = sierraItem.agent.locations ++ miroItem.agent.locations
          )
        )
      )
    )

    val expectedRedirectedWork =
      UnidentifiedRedirectedWork(miroWork, sierraPhysicalWork)

    result should contain theSameElementsAs List(
      expectedMergedWork,
      expectedRedirectedWork)
  }

  it("merges a Sierra digital work with a single-page Miro work") {
    val result = merger.merge(
      works = Seq(sierraDigitalWork, miroWork)
    )

    result.size shouldBe 2

    val expectedMergedWork = sierraDigitalWork.copy(
      otherIdentifiers = sierraDigitalWork.otherIdentifiers ++ miroWork.identifiers
    )

    val expectedRedirectedWork =
      UnidentifiedRedirectedWork(miroWork, sierraDigitalWork)

    result should contain theSameElementsAs List(
      expectedMergedWork,
      expectedRedirectedWork)
  }

  it(
    "merges a physical Sierra work and a digital Sierra work with a single-page Miro work") {
    val result = merger.merge(
      works = Seq(sierraPhysicalWork, sierraDigitalWork, miroWork)
    )

    result.size shouldBe 3

    val sierraItem =
      sierraPhysicalWork.items.head.asInstanceOf[Identifiable[Item]]
    val digitalItem = sierraDigitalWork.items.head

    val expectedMergedWork = sierraPhysicalWork.copy(
      otherIdentifiers = sierraPhysicalWork.otherIdentifiers ++ sierraDigitalWork.identifiers ++ miroWork.identifiers,
      items = List(
        sierraItem.copy(
          agent = sierraItem.agent.copy(
            locations = sierraItem.agent.locations ++ digitalItem.agent.locations
          )
        )
      )
    )

    val expectedRedirectedDigitalWork =
      UnidentifiedRedirectedWork(sierraDigitalWork, sierraPhysicalWork)

    val expectedMiroRedirectedWork =
      UnidentifiedRedirectedWork(miroWork, sierraPhysicalWork)

    result should contain theSameElementsAs List(
      expectedMergedWork,
      expectedRedirectedDigitalWork,
      expectedMiroRedirectedWork)
  }
}
