package uk.ac.wellcome.platform.merger.rules.physicaldigital

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.work.internal._
import uk.ac.wellcome.models.work.test.util.WorksGenerators
import uk.ac.wellcome.platform.merger.rules.physicaldigital.SierraPhysicalDigitalMergeRule.mergeAndRedirectWorks

class SierraPhysicalDigitalMergeRuleTest
    extends FunSpec
    with WorksGenerators
    with Matchers {
  describe("merges Sierra digital, physical work pairs") {
    it("merges a Sierra physical and a Sierra digital work") {
      val physicalWork = sierraPhysicalWorkWithOneItem
      val digitalWork = sierraDigitalWorkWithOneItem

      val result = mergeAndRedirectWorks(List(physicalWork, digitalWork))

      result shouldBe List(
        expectedMergedWork(physicalWork, digitalWork),
        UnidentifiedRedirectedWork(
          digitalWork,
          physicalWork))
    }

    it(
      "merges a Sierra digital and a Sierra physical work, order doesn't matter") {
      val physicalWork = sierraPhysicalWorkWithOneItem
      val digitalWork = sierraDigitalWorkWithOneItem

      val result = mergeAndRedirectWorks(
        List(digitalWork, physicalWork))

      result shouldBe List(
        expectedMergedWork(physicalWork, digitalWork),
        UnidentifiedRedirectedWork(
          digitalWork,
          physicalWork))
    }

    it("merges a Sierra physical and Sierra digital work, keeping extra works") {
      val isbnWork = createIsbnWork
      val result =
        mergeAndRedirectWorks(Seq(
          createSierraPhysicalWork,
          createSierraDigitalWork,
          isbnWork))

      result.size shouldBe 3

      result.collect { case r: UnidentifiedRedirectedWork => r }.size shouldBe 1
      result should contain(isbnWork)
    }
  }

  describe(
    "does not merge unless passed a single Sierra physical and single Sierra digital work") {
    it("does not merge a single Sierra digital work") {
      val works = List(sierraDigitalWorkWithOneItem)

      mergeAndRedirectWorks(works) shouldBe works
    }

    it("does not merge a single Sierra physical work") {
      val works = List(sierraPhysicalWorkWithOneItem)

      mergeAndRedirectWorks(works) shouldBe works
    }

    it("does not merge multiple Sierra physical works") {
      val works =
        List(sierraPhysicalWorkWithOneItem, sierraPhysicalWorkWithOneItem)

      mergeAndRedirectWorks(works) shouldBe works
    }

    it("does not merge multiple Sierra digital works") {
      val works =
        List(sierraDigitalWorkWithOneItem, sierraDigitalWorkWithOneItem)

      mergeAndRedirectWorks(works) shouldBe works
    }

    it("does not merge if there are no Sierra works") {
      val works = createIsbnWorks(5)

      mergeAndRedirectWorks(works) shouldBe works
    }

    it(
      "does not merge if there are multiple digital works with a single physical work") {
      val works = List(
        sierraDigitalWorkWithOneItem,
        sierraPhysicalWorkWithOneItem,
        createSierraDigitalWork)

      mergeAndRedirectWorks(works) shouldBe works
    }

    it(
      "does not merge if there are multiple physical works with a single digital work") {
      val works = List(
        sierraDigitalWorkWithOneItem,
        sierraPhysicalWorkWithOneItem,
        createSierraPhysicalWork)

      mergeAndRedirectWorks(works) shouldBe works
    }
  }

  describe("does not merge if item counts are wrong") {
    it("does not merge if physical work has 0 items") {
      val works = List(sierraWorkWithoutItems, sierraDigitalWorkWithOneItem)
      mergeAndRedirectWorks(works) shouldBe works
    }

    it("does not merge if physical work has >1 items") {
      val works =
        List(sierraWorkWithTwoPhysicalItems, sierraDigitalWorkWithOneItem)
      mergeAndRedirectWorks(works) shouldBe works
    }

    it("does not merge if digital work has 0 items") {
      val works = List(sierraPhysicalWorkWithOneItem, sierraWorkWithoutItems)
      mergeAndRedirectWorks(works) shouldBe works
    }

    it("does not merge if digital work has >1 items") {
      val works =
        List(sierraPhysicalWorkWithOneItem, sierraWorkWithTwoDigitalItems)
      mergeAndRedirectWorks(works) shouldBe works
    }
  }

  private def sierraPhysicalWorkWithOneItem: UnidentifiedWork = createSierraPhysicalWork
  private def sierraDigitalWorkWithOneItem: UnidentifiedWork = createSierraDigitalWork

  private def sierraWorkWithTwoPhysicalItems = createUnidentifiedSierraWorkWith(
    items = List(createPhysicalItem, createPhysicalItem)
  )
  private def sierraWorkWithTwoDigitalItems = createUnidentifiedSierraWorkWith(
    items = List(createDigitalItem, createDigitalItem)
  )
  private def sierraWorkWithoutItems = createUnidentifiedSierraWorkWith(
    items = List()
  )

  private def expectedMergedWork(physicalWork: UnidentifiedWork, digitalWork: UnidentifiedWork) = {
    val sierraPhysicalAgent = physicalWork.items.head.agent
    val sierraDigitalAgent = digitalWork.items.head.agent

    val expectedLocations = sierraPhysicalAgent.locations ++ sierraDigitalAgent.locations

    val expectedItem = physicalWork.items.head
      .asInstanceOf[Identifiable[Item]]
      .copy(
        agent = sierraPhysicalAgent.copy(
          locations = expectedLocations
        )
      )
    val expectedOtherIdentifiers = physicalWork.otherIdentifiers ++ digitalWork.identifiers

    val expectedMergedWork = physicalWork.copy(
      otherIdentifiers = expectedOtherIdentifiers,
      items = List(expectedItem)
    )
    expectedMergedWork
  }
}
