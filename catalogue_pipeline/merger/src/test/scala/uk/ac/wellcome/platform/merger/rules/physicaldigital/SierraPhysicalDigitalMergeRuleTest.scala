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
      val result = mergeAndRedirectWorks(
        List(sierraPhysicalWorkWithOneItem, sierraDigitalWorkWithOneItem))

      result shouldBe List(expectedMergedWork, expectedRedirectedWork)
    }

    it(
      "merges a Sierra digital and a Sierra physical work, order doesn't matter") {
      val result = mergeAndRedirectWorks(
        List(sierraDigitalWorkWithOneItem, sierraPhysicalWorkWithOneItem))

      result shouldBe List(expectedMergedWork, expectedRedirectedWork)
    }

    it("merges a Sierra physical and Sierra digital work, keeping extra works") {
      val physicalWork = createSierraPhysicalWork
      val digitalWork = createSierraDigitalWork
      val anotherWork = createIsbnWork

      val result =
        mergeAndRedirectWorks(Seq(physicalWork, digitalWork, anotherWork))

      result.size shouldBe 3

      result.collect { case r: UnidentifiedRedirectedWork => r }.size shouldBe 1
      result should contain(anotherWork)
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
        sierraDigitalWorkWithOneItem)

      mergeAndRedirectWorks(works) shouldBe works
    }

    it(
      "does not merge if there are multiple physical works with a single digital work") {
      val works = List(
        sierraDigitalWorkWithOneItem,
        sierraPhysicalWorkWithOneItem,
        sierraPhysicalWorkWithOneItem)

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

  val sierraPhysicalWorkWithOneItem: UnidentifiedWork = createSierraPhysicalWork
  val sierraDigitalWorkWithOneItem: UnidentifiedWork = createSierraDigitalWork

  private val sierraWorkWithTwoPhysicalItems = createUnidentifiedSierraWorkWith(
    items = List(createPhysicalItem, createPhysicalItem)
  )
  private val sierraWorkWithTwoDigitalItems = createUnidentifiedSierraWorkWith(
    items = List(createDigitalItem, createDigitalItem)
  )

  private val sierraWorkWithoutItems = createUnidentifiedSierraWorkWith(
    items = List()
  )

  private def expectedMergedWork = {
    val sierraPhysicalAgent = sierraPhysicalWorkWithOneItem.items.head.agent
    val sierraDigitalAgent = sierraDigitalWorkWithOneItem.items.head.agent

    val expectedLocations = sierraPhysicalAgent.locations ++ sierraDigitalAgent.locations

    val expectedItem = sierraPhysicalWorkWithOneItem.items.head
      .asInstanceOf[Identifiable[Item]]
      .copy(
        agent = sierraPhysicalAgent.copy(
          locations = expectedLocations
        )
      )
    val expectedIdentifiers = sierraPhysicalWorkWithOneItem.otherIdentifiers ++ sierraDigitalWorkWithOneItem.identifiers

    val expectedMergedWork = sierraPhysicalWorkWithOneItem.copy(
      otherIdentifiers = expectedIdentifiers,
      items = List(expectedItem)
    )
    expectedMergedWork
  }

  val expectedRedirectedWork = UnidentifiedRedirectedWork(
    sierraDigitalWorkWithOneItem,
    sierraPhysicalWorkWithOneItem)
}
