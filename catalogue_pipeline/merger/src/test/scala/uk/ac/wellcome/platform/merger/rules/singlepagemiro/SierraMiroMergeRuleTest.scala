package uk.ac.wellcome.platform.merger.rules.singlepagemiro

import org.scalatest.{Assertion, FunSpec, Matchers}
import uk.ac.wellcome.models.work.generators.WorksGenerators
import uk.ac.wellcome.models.work.internal._

class SierraMiroMergeRuleTest
    extends FunSpec
    with Matchers
    with WorksGenerators {

  describe("merges a single-page Miro work into a Sierra work") {
    it(
      "merges locations and identifiers from the Miro work into the Sierra work") {
      val sierraItem =
        createIdentifiableItemWith(locations = List(createPhysicalLocation))
      val sierraWork =
        createUnidentifiedSierraWorkWith(items = List(sierraItem))
      val miroWork = createMiroWork

      val result = mergeAndRedirectWorks(sierraWork, miroWork)

      val expectedMergedWork = sierraWork.copy(
        otherIdentifiers = sierraWork.otherIdentifiers ++ miroWork.identifiers,
        items = List(sierraItem.copy(
          agent = sierraItem.agent.copy(
            locations =
              sierraItem.agent.locations ++ miroWork.items.head.agent.locations
          )
        ))
      )

      result shouldBe List(
        expectedMergedWork,
        UnidentifiedRedirectedWork(miroWork, sierraWork))
    }

    it("merges a Miro work and a Sierra work, order does not matter") {
      val sierraItem =
        createIdentifiableItemWith(locations = List(createPhysicalLocation))
      val sierraWork =
        createUnidentifiedSierraWorkWith(items = List(sierraItem))
      val miroWork = createMiroWork

      val result = mergeAndRedirectWorks(miroWork, sierraWork)

      val expectedMergedWork = sierraWork.copy(
        otherIdentifiers = sierraWork.otherIdentifiers ++ miroWork.identifiers,
        items = List(sierraItem.copy(
          agent = sierraItem.agent.copy(
            locations =
              sierraItem.agent.locations ++ miroWork.items.head.agent.locations
          )
        ))
      )

      result shouldBe List(
        expectedMergedWork,
        UnidentifiedRedirectedWork(miroWork, sierraWork))
    }

    it(
      "merges otherIdentifiers from the Miro work, except for any Sierra identifiers, to the Sierra work") {
      val sierraItem =
        createIdentifiableItemWith(locations = List(createPhysicalLocation))
      val sierraWork =
        createUnidentifiedSierraWorkWith(items = List(sierraItem))

      val miroLibraryReferenceSourceIdentifier = createSourceIdentifierWith(
        identifierType = IdentifierType("miro-library-reference")
      )
      val miroWork = createMiroWorkWith(
        otherIdentifiers = List(
          miroLibraryReferenceSourceIdentifier,
          sierraWork.sourceIdentifier,
          createSierraIdentifierSourceIdentifier,
          createSierraSystemSourceIdentifier))

      val result = mergeAndRedirectWorks(sierraWork, miroWork)

      val expectedMergedWork = sierraWork.copy(
        otherIdentifiers =
          sierraWork.otherIdentifiers :+
            miroWork.sourceIdentifier :+
            miroLibraryReferenceSourceIdentifier,
        items = List(sierraItem.copy(
          agent = sierraItem.agent.copy(
            locations =
              sierraItem.agent.locations ++ miroWork.items.head.agent.locations
          )
        ))
      )

      result shouldBe List(
        expectedMergedWork,
        UnidentifiedRedirectedWork(miroWork, sierraWork))
    }

    it(
      "does not merge Miro DigitalLocation if the Sierra work already has a DigitalLocation") {
      val sierraItem =
        createIdentifiableItemWith(locations = List(createDigitalLocation))
      val sierraWork =
        createUnidentifiedSierraWorkWith(items = List(sierraItem))
      val miroWork = createMiroWork

      val result = mergeAndRedirectWorks(sierraWork, miroWork)

      val expectedMergedWork = sierraWork.copy(
        otherIdentifiers = sierraWork.otherIdentifiers ++ miroWork.identifiers
      )

      result shouldBe List(
        expectedMergedWork,
        UnidentifiedRedirectedWork(miroWork, sierraWork))
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

  private def assertWorksAreNotMerged(works: BaseWork*): Assertion =
    assertWorksAreNotMerged(works)

  private def assertWorksAreNotMerged(works: Seq[BaseWork]): Assertion =
    mergeAndRedirectWorks(works) shouldBe works

  private def mergeAndRedirectWorks(works: BaseWork*): Seq[BaseWork] =
    mergeAndRedirectWorks(works)

  private def mergeAndRedirectWorks(works: Seq[BaseWork]): Seq[BaseWork] =
    SierraMiroMergeRule.mergeAndRedirectWorks(works)
}
