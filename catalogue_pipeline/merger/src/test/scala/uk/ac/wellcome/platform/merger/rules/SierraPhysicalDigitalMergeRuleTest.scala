package uk.ac.wellcome.platform.merger.rules

import org.scalatest.FunSpec
import uk.ac.wellcome.models.work.internal._
import uk.ac.wellcome.platform.merger.MergerTestUtils
import uk.ac.wellcome.platform.merger.rules.SierraPhysicalDigitalMergeRule.mergeAndRedirectWork

class SierraPhysicalDigitalMergeRuleTest extends FunSpec with MergerTestUtils {
  val physicalWorkWithOneItem: UnidentifiedWork = createPhysicalWork
  val digitalWorkWithOneItem: UnidentifiedWork = createDigitalWorkWith(otherIdentifiers = List(createSourceIdentifier))

  it("merges a physical and a digital work") {
    val result = mergeAndRedirectWork(List(physicalWorkWithOneItem, digitalWorkWithOneItem))

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

    val expectedIdentifiers = physicalWorkWithOneItem.otherIdentifiers ++ digitalWorkWithOneItem.identifiers

    val expectedMergedWork = physicalWorkWithOneItem.copy(
      otherIdentifiers =
        expectedIdentifiers,
      items = List(expectedItem)
    )

    val expectedRedirectedWork = UnidentifiedRedirectedWork(
      sourceIdentifier = digitalWorkWithOneItem.sourceIdentifier,
      version = digitalWorkWithOneItem.version,
      redirect = IdentifiableRedirect(physicalWorkWithOneItem.sourceIdentifier))

    result shouldBe List(expectedMergedWork, expectedRedirectedWork)
  }

  it("merges a physical and digital work, even if there are extra works") {
    val physicalWork = createPhysicalWork
    val digitalWork = createDigitalWork

    val result = mergeAndRedirectWork(Seq(
        physicalWork,
        digitalWork,
        createUnidentifiedWorkWith(
          sourceIdentifier = createSourceIdentifierWith(
            identifierType = "miro-image-number"
          )
        )
      )
    )

    result.size shouldBe 3
    result.collect { case r: UnidentifiedRedirectedWork => r }.size shouldBe 1
  }

  describe("filters physical and digital works") {
    it("merges a physical and digital work") {
      val physicalWork = createPhysicalWork
      val digitalWork = createDigitalWork

      val result = mergeAndRedirectWork(Seq(physicalWork, digitalWork))

      result.size shouldBe 2

      val physicalItem =
        physicalWork.items.head.asInstanceOf[Identifiable[Item]]
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


    it("ignores a single physical work") {
      val works = Seq(createPhysicalWork)
      mergeAndRedirectWork(works) shouldBe works
    }

    it("ignores a single digital work") {
      val works = Seq(createDigitalWork)
      mergeAndRedirectWork(works) shouldBe works
    }

    it("ignores multiple physical works") {
      val works = (1 to 3).map { _ =>
        createPhysicalWork
      }
      mergeAndRedirectWork(works) shouldBe works
    }

    it("ignores multiple digital works") {
      val works = (1 to 3).map { _ =>
        createDigitalWork
      }
      mergeAndRedirectWork(works) shouldBe works
    }

    it("ignores multiple physical works with a single digital work") {
      val works = (1 to 3).map { _ =>
        createPhysicalWork
      } ++ Seq(createDigitalWork)
      mergeAndRedirectWork(works) shouldBe works
    }

    it("ignores multiple digital works with a single physical work") {
      val works = (1 to 3).map { _ =>
        createDigitalWork
      } ++ Seq(createPhysicalWork)
      mergeAndRedirectWork(works) shouldBe works
    }

    it("ignores a physical work with multiple items") {
      val works = List(
        createPhysicalWorkWith(
          items = List(createPhysicalItem, createPhysicalItem)
        ),
        createDigitalWork
      )
      mergeAndRedirectWork(works) shouldBe works
    }

    it("ignores a digital work with multiple items") {
      val works = List(
        createPhysicalWork,
        createDigitalWorkWith(
          items = List(createDigitalItem, createDigitalItem)
        )
      )
      mergeAndRedirectWork(works) shouldBe works
    }
  }
  
  describe("returns works unchanged if item counts are wrong") {
    it("does not merge if physical work has 0 items") {
      val physicalWork = createUnidentifiedWorkWith(
        items = List()
      )

      val works = List(physicalWork, digitalWorkWithOneItem)
      mergeAndRedirectWork(works) shouldBe works
    }

    it("does not merge if physical work has >1 items") {
      val physicalWork = createUnidentifiedWorkWith(
        items = List(createPhysicalItem, createPhysicalItem)
      )

      val works = List(physicalWork, digitalWorkWithOneItem)
      mergeAndRedirectWork(works) shouldBe works
    }

    it("does not merge if digital work has 0 items") {
      val digitalWork = createUnidentifiedWorkWith(
        items = List()
      )

      val works = List(physicalWorkWithOneItem, digitalWork)
      mergeAndRedirectWork(works) shouldBe works
    }

    it("does not merge if digital work has >1 items") {
      val digitalWork = createUnidentifiedWorkWith(
        items = List(createDigitalItem, createDigitalItem)
      )

      val works = List(physicalWorkWithOneItem, digitalWork)
      mergeAndRedirectWork(works) shouldBe works
    }
  }

  private def createPhysicalItem: Identifiable[Item] =
    createIdentifiableItemWith(locations = List(createPhysicalLocation))

  private def createDigitalItem: Unidentifiable[Item] =
    createUnidentifiableItemWith(locations = List(createDigitalLocation))
  
}
