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
          sourceIdentifier = miroWork.sourceIdentifier,
          version = miroWork.version,
          redirect = IdentifiableRedirect(sierraPhysicalWork.sourceIdentifier))
      }

      it("copies identifiers from the Miro work to the Sierra work") {
        mergedWork.sourceIdentifier shouldBe sierraPhysicalWork.sourceIdentifier
        mergedWork.otherIdentifiers shouldBe sierraPhysicalWork.otherIdentifiers ++ miroWork.identifiers
      }

      it("doesn't copy Sierra identifiers from the Miro work") {
        val miroLibraryReferenceSourceIdentifier = createSourceIdentifierWith(
          identifierType = IdentifierType("miro-library-reference")
        )
        val miroWorkWithExtraIdentifiers = createMiroWorkWith(
          otherIdentifiers = List(
            miroLibraryReferenceSourceIdentifier,
            sierraPhysicalWork.sourceIdentifier,
            createSierraIdentifierSourceIdentifier,
            createSierraSystemSourceIdentifier))

        val mergedWork =
          getMergedWork(sierraPhysicalWork, miroWorkWithExtraIdentifiers)

        mergedWork.otherIdentifiers shouldBe sierraPhysicalWork.otherIdentifiers :+
          miroWorkWithExtraIdentifiers.sourceIdentifier :+
          miroLibraryReferenceSourceIdentifier
      }

      it("copies item locations from the Miro work to a physical Sierra work") {
        mergedWork.items shouldBe List(
          sierraPhysicalItem.copy(
            agent = sierraPhysicalItem.agent.copy(
              locations =
                sierraPhysicalItem.agent.locations ++ miroWork.items.head.agent.locations
            )
          ))
      }

      it("copies across the Miro location if the Sierra work already has a DigitalLocation") {
        val mergedWork = getMergedWork(sierraDigitalWork, miroWork)

        mergedWork.items shouldBe List(
          sierraDigitalItem.copy(
            agent = sierraDigitalItem.agent.copy(
              locations =
                sierraDigitalItem.agent.locations ++ miroWork.items.head.agent.locations
            )
          ))
      }

      it("copies across the thumbnail from the Miro work") {
        miroWork.thumbnail.isDefined shouldBe true
        mergedWork.thumbnail.isDefined shouldBe true
        mergedWork.thumbnail shouldBe miroWork.thumbnail
      }
    }

    describe("order: sierraWork, miroWork (order doesn't matter)") {
      val redirectedWork = getRedirectedWork(miroWork, sierraPhysicalWork)
      val mergedWork = getMergedWork(miroWork, sierraPhysicalWork)

      it("creates a redirect from the Miro work to the Sierra work") {
        redirectedWork shouldBe UnidentifiedRedirectedWork(
          sourceIdentifier = miroWork.sourceIdentifier,
          version = miroWork.version,
          redirect = IdentifiableRedirect(sierraPhysicalWork.sourceIdentifier))
      }

      it("copies identifiers from the Miro work to the Sierra work") {
        mergedWork.sourceIdentifier shouldBe sierraPhysicalWork.sourceIdentifier
        mergedWork.otherIdentifiers shouldBe sierraPhysicalWork.otherIdentifiers ++ miroWork.identifiers
      }

      it("copies item locations from the Miro work to a physical Sierra work") {
        mergedWork.items shouldBe List(
          sierraPhysicalItem.copy(
            agent = sierraPhysicalItem.agent.copy(
              locations =
                sierraPhysicalItem.agent.locations ++ miroWork.items.head.agent.locations
            )
          ))
      }
    }

    def getMergedWork(works: BaseWork*): UnidentifiedWork = {
      val result = mergeAndRedirectWorks(works)

      result should have size 2
      result.head shouldBe a[UnidentifiedWork]
      result.tail.head shouldBe a[UnidentifiedRedirectedWork]

      result.head.asInstanceOf[UnidentifiedWork]
    }

    def getRedirectedWork(works: BaseWork*): UnidentifiedRedirectedWork = {
      val result = mergeAndRedirectWorks(works)

      result should have size 2
      result.head shouldBe a[UnidentifiedWork]
      result.tail.head shouldBe a[UnidentifiedRedirectedWork]

      result.tail.head.asInstanceOf[UnidentifiedRedirectedWork]
    }
  }

  describe("does not merge unless passed exactly one Sierra and one Miro work") {
    it("does not merge a single Sierra  work") {
      assertWorksAreNotMerged(createUnidentifiedSierraWork)
    }

    it("does not merge a single Sierra physical work") {
      assertWorksAreNotMerged(createMiroWork)
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
      assertWorksAreNotMerged(createIsbnWorks(5): _*)
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
    mergeAndRedirectWorks(works) shouldBe works

  private def mergeAndRedirectWorks(works: Seq[BaseWork]): Seq[BaseWork] = {
    val result = SierraMiroMergeRule.mergeAndRedirectWorks(works)

    // We always get the same number of works as we started with.
    result.size shouldBe works.size

    // Stronger: the source identifiers are preserved on the way through.
    result.map { _.sourceIdentifier } should contain theSameElementsAs works
      .map { _.sourceIdentifier }

    result
  }
}
