package uk.ac.wellcome.platform.merger.rules.physicaldigital

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.work.generators.WorksGenerators
import uk.ac.wellcome.models.work.internal._
import uk.ac.wellcome.platform.merger.rules.physicaldigital.SierraPhysicalDigitalMergeRule.mergeAndRedirectWorks

class SierraPhysicalDigitalMergeRuleTest
    extends FunSpec
    with WorksGenerators
    with Matchers {
  describe("merges Sierra digital, physical work pairs") {
    it("merges a Sierra physical and a Sierra digital work") {
      val physicalWork = createSierraPhysicalWork
      val digitalWork = createSierraDigitalWork

      val result = mergeAndRedirectWorks(List(physicalWork, digitalWork))

      result shouldBe List(
        expectedMergedWork(physicalWork, digitalWork),
        UnidentifiedRedirectedWork(
          sourceIdentifier = digitalWork.sourceIdentifier,
          version = digitalWork.version,
          redirect = IdentifiableRedirect(physicalWork.sourceIdentifier))
      )
    }

    it(
      "merges a Sierra digital and a Sierra physical work, order doesn't matter") {
      val physicalWork = createSierraPhysicalWork
      val digitalWork = createSierraDigitalWork

      val result = mergeAndRedirectWorks(List(digitalWork, physicalWork))

      result shouldBe List(
        expectedMergedWork(physicalWork, digitalWork),
        UnidentifiedRedirectedWork(
          sourceIdentifier = digitalWork.sourceIdentifier,
          version = digitalWork.version,
          redirect = IdentifiableRedirect(physicalWork.sourceIdentifier))
      )
    }

    it("merges a Sierra physical and Sierra digital work, keeping extra works") {
      val isbnWorks = createIsbnWorks(2)
      val result =
        mergeAndRedirectWorks(
          Seq(
            createSierraPhysicalWork,
            createSierraDigitalWork,
          ) ++ isbnWorks)

      result.size shouldBe 4

      result.collect { case r: UnidentifiedRedirectedWork => r }.size shouldBe 1
      result should contain allElementsOf isbnWorks
    }
  }

  describe(
    "does not merge unless passed a single Sierra physical and single Sierra digital work") {
    it("does not merge a single Sierra digital work") {
      val works = List(createSierraDigitalWork)

      mergeAndRedirectWorks(works) shouldBe works
    }

    it("does not merge a single Sierra physical work") {
      val works = List(createSierraPhysicalWork)

      mergeAndRedirectWorks(works) shouldBe works
    }

    it("does not merge multiple Sierra physical works") {
      val works =
        List(createSierraPhysicalWork, createSierraPhysicalWork)

      mergeAndRedirectWorks(works) shouldBe works
    }

    it("does not merge multiple Sierra digital works") {
      val works =
        List(createSierraDigitalWork, createSierraDigitalWork)

      mergeAndRedirectWorks(works) shouldBe works
    }

    it("does not merge if there are no Sierra works") {
      val works = createIsbnWorks(5)

      mergeAndRedirectWorks(works) shouldBe works
    }

    it(
      "does not merge if there are multiple digital works with a single physical work") {
      val works = List(
        createSierraDigitalWork,
        createSierraPhysicalWork,
        createSierraDigitalWork)

      mergeAndRedirectWorks(works) shouldBe works
    }

    it(
      "does not merge if there are multiple physical works with a single digital work") {
      val works = List(
        createSierraDigitalWork,
        createSierraPhysicalWork,
        createSierraPhysicalWork)

      mergeAndRedirectWorks(works) shouldBe works
    }
  }

  describe("does not merge if item counts are wrong") {
    it("does not merge if physical work has 0 items") {
      val works = List(createSierraWorkWithoutItems, createSierraDigitalWork)
      mergeAndRedirectWorks(works) shouldBe works
    }

    it("does not merge if physical work has >1 items") {
      val works =
        List(createSierraWorkWithTwoPhysicalItems, createSierraDigitalWork)
      mergeAndRedirectWorks(works) shouldBe works
    }

    it("does not merge if digital work has 0 items") {
      val works = List(createSierraPhysicalWork, createSierraWorkWithoutItems)
      mergeAndRedirectWorks(works) shouldBe works
    }

    it("does not merge if digital work has >1 items") {
      val works =
        List(createSierraPhysicalWork, createSierraWorkWithTwoDigitalItems)
      mergeAndRedirectWorks(works) shouldBe works
    }
  }

  private def createSierraWorkWithTwoPhysicalItems =
    createUnidentifiedSierraWorkWith(
      items = List(createPhysicalItem, createPhysicalItem)
    )
  private def createSierraWorkWithTwoDigitalItems =
    createUnidentifiedSierraWorkWith(
      items = List(createDigitalItem, createDigitalItem)
    )
  private def createSierraWorkWithoutItems = createUnidentifiedSierraWorkWith(
    items = List()
  )

  private def expectedMergedWork(physicalWork: UnidentifiedWork,
                                 digitalWork: UnidentifiedWork) = {
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
      items = List(expectedItem),
      merged = true
    )
    expectedMergedWork
  }
}
