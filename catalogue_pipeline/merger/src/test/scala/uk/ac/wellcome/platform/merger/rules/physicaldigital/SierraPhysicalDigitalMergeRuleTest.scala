package uk.ac.wellcome.platform.merger.rules.physicaldigital

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.work.internal._
import uk.ac.wellcome.models.work.test.util.WorksGenerators
import uk.ac.wellcome.platform.merger.rules.physicaldigital.SierraPhysicalDigitalMergeRule.mergeAndRedirectWork

class SierraPhysicalDigitalMergeRuleTest extends FunSpec with WorksGenerators with Matchers {
  val sierraPhysicalWorkWithOneItem: UnidentifiedWork = createSierraPhysicalWork

  val sierraDigitalWorkWithOneItem: UnidentifiedWork = createSierraDigitalWork

  private val sierraWorkWithTwoPhysicalItems = createSierraWorkWith(
    items = List(createPhysicalItem, createPhysicalItem)
  )
  private val sierraWorkWithTwoDigitalItems = createSierraWorkWith(
    items = List(createDigitalItem, createDigitalItem)
  )

  private val sierraWorkWithoutItems = createSierraWorkWith(
    items = List()
  )

  it("merges a Sierra physical and a Sierra digital work") {
    val result = mergeAndRedirectWork(
      List(sierraPhysicalWorkWithOneItem, sierraDigitalWorkWithOneItem))

    val expectedLocations =
      sierraPhysicalWorkWithOneItem.items.head.agent.locations ++
        sierraDigitalWorkWithOneItem.items.head.agent.locations

    val expectedItem = sierraPhysicalWorkWithOneItem.items.head
      .asInstanceOf[Identifiable[Item]]
      .copy(
        agent = sierraPhysicalWorkWithOneItem.items.head.agent.copy(
          locations = expectedLocations
        )
      )
    val expectedIdentifiers = sierraPhysicalWorkWithOneItem.otherIdentifiers ++ sierraDigitalWorkWithOneItem.identifiers

    val expectedMergedWork = sierraPhysicalWorkWithOneItem.copy(
      otherIdentifiers = expectedIdentifiers,
      items = List(expectedItem)
    )

    val expectedRedirectedWork = UnidentifiedRedirectedWork(
      sourceIdentifier = sierraDigitalWorkWithOneItem.sourceIdentifier,
      version = sierraDigitalWorkWithOneItem.version,
      redirect = IdentifiableRedirect(sierraPhysicalWorkWithOneItem.sourceIdentifier)
    )

    result shouldBe List(expectedMergedWork, expectedRedirectedWork)
  }

  it("merges a physical and digital work, even if there are extra works") {
    val physicalWork = createSierraPhysicalWork
    val digitalWork = createSierraDigitalWork
    val anotherWork = createIsbnWork

    val result = mergeAndRedirectWork(
      Seq(
        physicalWork,
        digitalWork,
        anotherWork))

    result.size shouldBe 3
    println(result)
    result.collect { case r: UnidentifiedRedirectedWork => r }.size shouldBe 1
  }

  it("does not merge if there are no Sierra items") {
    val works = createIsbnWorks(5)

    val result = mergeAndRedirectWork(works)

    result shouldBe works
  }

  describe("returns works unchanged if item counts are wrong") {
    it("does not merge if physical work has 0 items") {
      val works = List(sierraWorkWithoutItems, sierraDigitalWorkWithOneItem)
      mergeAndRedirectWork(works) shouldBe works
    }

    it("does not merge if physical work has >1 items") {
      val works = List(sierraWorkWithTwoPhysicalItems, sierraDigitalWorkWithOneItem)
      mergeAndRedirectWork(works) shouldBe works
    }

    it("does not merge if digital work has 0 items") {
      val works = List(sierraPhysicalWorkWithOneItem, sierraWorkWithoutItems)
      mergeAndRedirectWork(works) shouldBe works
    }

    it("does not merge if digital work has >1 items") {
      val works = List(sierraPhysicalWorkWithOneItem, sierraWorkWithTwoDigitalItems)
      mergeAndRedirectWork(works) shouldBe works
    }
  }
}
