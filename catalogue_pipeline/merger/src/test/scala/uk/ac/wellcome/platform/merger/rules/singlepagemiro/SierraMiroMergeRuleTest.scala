package uk.ac.wellcome.platform.merger.rules.singlepagemiro

import org.scalatest.{Assertion, FunSpec, Matchers}
import uk.ac.wellcome.models.work.generators.WorksGenerators
import uk.ac.wellcome.models.work.internal._

class SierraMiroMergeRuleTest
    extends FunSpec
    with Matchers
    with WorksGenerators {

  describe("merges a single-page Miro work into a Sierra work") {
    val sierraPhysicalItem =
      createIdentifiableItemWith(locations = List(createPhysicalLocation))

    val sierraPhysicalWork = createUnidentifiedSierraWorkWith(
      items = List(sierraPhysicalItem)
    )

    val sierraDigitalItem = createIdentifiableItemWith(
      locations = List(createDigitalLocation)
    )
    val sierraDigitalWork = createUnidentifiedSierraWorkWith(
      items = List(sierraDigitalItem)
    )

    val miroWork = createMiroWork

    describe("order: miroWork, sierraWork") {
      val redirectedWork = getRedirectedWork(sierraPhysicalWork, miroWork)
      val mergedWork = getMergedWork(sierraPhysicalWork, miroWork)

      it("creates a redirect from the Miro work to the Sierra work") {
        redirectedWork shouldBe UnidentifiedRedirectedWork(
          source = miroWork,
          target = sierraPhysicalWork
        )
      }

      it("copies identifiers from the Miro work to the Sierra work") {
        mergedWork.identifiers shouldBe sierraPhysicalWork.otherIdentifiers ++ miroWork.identifiers
      }

      it("copies item locations from the Miro work to a physical Sierra work") {
        mergedWork.items shouldBe List(sierraPhysicalItem.copy(
          agent = sierraPhysicalItem.agent.copy(
            locations =
              sierraPhysicalItem.agent.locations ++ miroWork.items.head.agent.locations
          )
        ))
      }
    }

    describe("order: sierraWork, miroWork (order doesn't matter)") {
      val redirectedWork = getRedirectedWork(miroWork, sierraPhysicalWork)
      val mergedWork = getMergedWork(miroWork, sierraPhysicalWork)

      it("creates a redirect from the Miro work to the Sierra work") {
        redirectedWork shouldBe UnidentifiedRedirectedWork(
          source = miroWork,
          target = sierraPhysicalWork
        )
      }

      it("copies identifiers from the Miro work to the Sierra work") {
        mergedWork.identifiers shouldBe sierraPhysicalWork.otherIdentifiers ++ miroWork.identifiers
      }

      it("copies item locations from the Miro work to a physical Sierra work") {
        mergedWork.items shouldBe List(sierraPhysicalItem.copy(
          agent = sierraPhysicalItem.agent.copy(
            locations =
              sierraPhysicalItem.agent.locations ++ miroWork.items.head.agent.locations
          )
        ))
      }
    }

    it(
      "merges otherIdentifiers from the Miro work, except for any Sierra identifiers, to the Sierra work") {
      val miroLibraryReferenceSourceIdentifier = createSourceIdentifierWith(
        identifierType = IdentifierType("miro-library-reference")
      )
      val miroWorkWithExtraIdentifiers = createMiroWorkWith(
        otherIdentifiers = List(
          miroLibraryReferenceSourceIdentifier,
          sierraPhysicalWork.sourceIdentifier,
          createSierraIdentifierSourceIdentifier,
          createSierraSystemSourceIdentifier))

      val result = mergeAndRedirectWorks(sierraPhysicalWork, miroWorkWithExtraIdentifiers)

      val expectedMergedWork = sierraPhysicalWork.copy(
        otherIdentifiers =
          sierraPhysicalWork.otherIdentifiers :+
            miroWorkWithExtraIdentifiers.sourceIdentifier :+
            miroLibraryReferenceSourceIdentifier,
        items = List(sierraPhysicalItem.copy(
          agent = sierraPhysicalItem.agent.copy(
            locations =
              sierraPhysicalItem.agent.locations ++ miroWorkWithExtraIdentifiers.items.head.agent.locations
          )
        ))
      )

      result shouldBe List(
        expectedMergedWork,
        UnidentifiedRedirectedWork(miroWorkWithExtraIdentifiers, sierraPhysicalWork))
    }

    it(
      "does not merge Miro DigitalLocation if the Sierra work already has a DigitalLocation") {
      val result = mergeAndRedirectWorks(sierraDigitalWork, miroWork)

      val expectedMergedWork = sierraDigitalWork.copy(
        otherIdentifiers = sierraDigitalWork.otherIdentifiers ++ miroWork.identifiers
      )

      result shouldBe List(
        expectedMergedWork,
        UnidentifiedRedirectedWork(miroWork, sierraDigitalWork))
    }
  }

  describe("does not merge unless passed a Sierra and a Miro work") {
    it("does not merge a single Sierra  work") {
      assertWorksAreNotMerged(Seq(createUnidentifiedSierraWork))
    }

    it("does not merge a single Sierra physical work") {
      assertWorksAreNotMerged(Seq(createMiroWork))
    }

    it("does not merge multiple Sierra works") {
      assertWorksAreNotMerged(
        createUnidentifiedSierraWork,
        createUnidentifiedSierraWork)
    }

    it("does not merge multiple Miro works") {
      assertWorksAreNotMerged(createMiroWork, createMiroWork)
    }

    it("does not merge if there are no Sierra works") {
      assertWorksAreNotMerged(createIsbnWorks(5))
    }

    it(
      "does not merge if there are multiple Sierra works with a single Miro work") {
      assertWorksAreNotMerged(
        createMiroWork,
        createUnidentifiedSierraWork,
        createUnidentifiedSierraWork)
    }

    it(
      "does not merge if there are multiple Miro works with a single Sierra work") {
      assertWorksAreNotMerged(
        createMiroWork,
        createMiroWork,
        createUnidentifiedSierraWork
      )
    }
  }

  def getMergedWork(works: BaseWork*): UnidentifiedWork = {
    val result = mergeAndRedirectWorks(works)

    result should have size 2
    result.head shouldBe a[UnidentifiedWork]
    result.tail.head shouldBe a[UnidentifiedRedirectedWork]

    result.head
  }

  def getRedirectedWork(works: BaseWork*): UnidentifiedRedirectedWork = {
    val result = mergeAndRedirectWorks(works)

    result should have size 2
    result.head shouldBe a[UnidentifiedWork]
    result.tail.head shouldBe a[UnidentifiedRedirectedWork]

    result.tail.head
  }

  private def assertWorksAreNotMerged(works: BaseWork*): Assertion =
    assertWorksAreNotMerged(works)

  private def assertWorksAreNotMerged(works: Seq[BaseWork]): Assertion =
    mergeAndRedirectWorks(works) shouldBe works

  private def mergeAndRedirectWorks(works: BaseWork*): Seq[BaseWork] =
    mergeAndRedirectWorks(works)

  private def mergeAndRedirectWorks(works: Seq[BaseWork]): Seq[BaseWork] =
    SierraMiroMergeRule.mergeAndRedirectWorks(works)
}
