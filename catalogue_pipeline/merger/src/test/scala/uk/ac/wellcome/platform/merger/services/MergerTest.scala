package uk.ac.wellcome.platform.merger.services

import org.scalatest.FunSpec
import uk.ac.wellcome.models.work.internal._
import uk.ac.wellcome.platform.merger.MergerTestUtils

class MergerTest extends FunSpec with MergerTestUtils {

  private val merger = new Merger()

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

    result should contain theSameElementsAs List(
      expectedMergedWork,
      expectedRedirectedWork)
  }

  private def createPhysicalItem: Identifiable[Item] =
    createIdentifiableItemWith(locations = List(createPhysicalLocation))

  private def createDigitalItem: Unidentifiable[Item] =
    createUnidentifiableItemWith(locations = List(createDigitalLocation))
}
