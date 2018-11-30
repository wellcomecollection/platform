package uk.ac.wellcome.platform.merger.services

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.work.generators.WorksGenerators
import uk.ac.wellcome.models.work.internal._

class MergerTest extends FunSpec with WorksGenerators with Matchers {
  private val sierraPhysicalWork = createSierraPhysicalWork
  private val sierraDigitalWork = createSierraDigitalWork
  private val miroWork = createMiroWork

  private val merger = PlatformMerger

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
      ),
      merged = true
    )

    val expectedRedirectedWork =
      UnidentifiedRedirectedWork(
        sourceIdentifier = sierraDigitalWork.sourceIdentifier,
        version = sierraDigitalWork.version,
        redirect = IdentifiableRedirect(sierraPhysicalWork.sourceIdentifier)
      )

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
      thumbnail = miroWork.thumbnail,
      items = List(
        sierraItem.copy(
          agent = sierraItem.agent.copy(
            locations = sierraItem.agent.locations ++ miroItem.agent.locations
          )
        )
      ),
      merged = true
    )

    val expectedRedirectedWork =
      UnidentifiedRedirectedWork(
        sourceIdentifier = miroWork.sourceIdentifier,
        version = miroWork.version,
        redirect = IdentifiableRedirect(sierraPhysicalWork.sourceIdentifier))

    result should contain theSameElementsAs List(
      expectedMergedWork,
      expectedRedirectedWork)
  }

  it("merges a Sierra digital work with a single-page Miro work") {
    val result = merger.merge(
      works = Seq(sierraDigitalWork, miroWork)
    )

    result.size shouldBe 2

    val sierraItem =
      sierraDigitalWork.items.head.asInstanceOf[Unidentifiable[Item]]
    val miroItem = miroWork.items.head

    val expectedMergedWork = sierraDigitalWork.copy(
      otherIdentifiers = sierraDigitalWork.otherIdentifiers ++ miroWork.identifiers,
      thumbnail = miroWork.thumbnail,
      items = List(
        sierraItem.copy(
          agent = sierraItem.agent.copy(
            locations = sierraItem.agent.locations ++ miroItem.agent.locations
          )
        )
      ),
      merged = true
    )

    val expectedRedirectedWork =
      UnidentifiedRedirectedWork(
        sourceIdentifier = miroWork.sourceIdentifier,
        version = miroWork.version,
        redirect = IdentifiableRedirect(sierraDigitalWork.sourceIdentifier))

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
    val miroItem = miroWork.items.head

    val expectedMergedWork = sierraPhysicalWork.copy(
      otherIdentifiers = sierraPhysicalWork.otherIdentifiers ++ sierraDigitalWork.identifiers ++ miroWork.identifiers,
      thumbnail = miroWork.thumbnail,
      items = List(
        sierraItem.copy(
          agent = sierraItem.agent.copy(
            locations = sierraItem.agent.locations ++ digitalItem.agent.locations ++ miroItem.agent.locations
          )
        )
      ),
      merged = true
    )

    val expectedRedirectedDigitalWork =
      UnidentifiedRedirectedWork(
        sourceIdentifier = sierraDigitalWork.sourceIdentifier,
        version = sierraDigitalWork.version,
        redirect = IdentifiableRedirect(sierraPhysicalWork.sourceIdentifier)
      )

    val expectedMiroRedirectedWork =
      UnidentifiedRedirectedWork(
        sourceIdentifier = miroWork.sourceIdentifier,
        version = miroWork.version,
        redirect = IdentifiableRedirect(sierraPhysicalWork.sourceIdentifier))

    result should contain theSameElementsAs List(
      expectedMergedWork,
      expectedRedirectedDigitalWork,
      expectedMiroRedirectedWork)
  }
}
