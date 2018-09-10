package uk.ac.wellcome.platform.merger.rules.digitalphysical

import org.scalatest.FunSpec
import uk.ac.wellcome.models.work.internal._
import uk.ac.wellcome.platform.merger.MergerTestUtils
import uk.ac.wellcome.platform.merger.rules.digitalphysical.SierraPhysicalDigitalMergeRule.mergeAndRedirectWork

class SierraPhysicalDigitalMergeRuleTest extends FunSpec with MergerTestUtils {
  val physicalWorkWithOneItem: UnidentifiedWork = createPhysicalSierraWork
  val digitalWorkWithOneItem: UnidentifiedWork = createDigitalSierraWork

  it("merges a physical and a digital work") {
    val result = mergeAndRedirectWork(
      List(physicalWorkWithOneItem, digitalWorkWithOneItem))

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
      otherIdentifiers = expectedIdentifiers,
      items = List(expectedItem)
    )

    val expectedRedirectedWork = UnidentifiedRedirectedWork(
      sourceIdentifier = digitalWorkWithOneItem.sourceIdentifier,
      version = digitalWorkWithOneItem.version,
      redirect = IdentifiableRedirect(physicalWorkWithOneItem.sourceIdentifier)
    )

    result shouldBe List(expectedMergedWork, expectedRedirectedWork)
  }

  it("merges a physical and digital work, even if there are extra works") {
    val physicalWork = createPhysicalSierraWork
    val digitalWork = createDigitalSierraWork

    val result = mergeAndRedirectWork(
      Seq(
        physicalWork,
        digitalWork,
        createUnidentifiedWorkWith(
          sourceIdentifier = createSourceIdentifierWith(
            identifierType = "miro-image-number"
          )
        )
      ))

    result.size shouldBe 3
    result.collect { case r: UnidentifiedRedirectedWork => r }.size shouldBe 1
  }

  it("does not merge if there are no physical or digital items") {
    val works = createUnidentifiedWorks(5)

    val result = mergeAndRedirectWork(works)

    result shouldBe works
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
}
