package uk.ac.wellcome.platform.merger.rules.singlepagemiro

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.work.internal._
import uk.ac.wellcome.models.work.test.util.WorksGenerators
import uk.ac.wellcome.platform.merger.rules.singlepagemiro.SierraMiroMergeRule.mergeAndRedirectWork

class SierraMiroMergeRuleTest
    extends FunSpec
    with Matchers
    with WorksGenerators {

  describe("Merges a single-page Miro work into a Sierra work") {
    it("merges a minimal Miro work") {
      val sierraItem =
        createIdentifiableItemWith(locations = List(createPhysicalLocation))
      val sierraWork = createSierraWorkWith(items = List(sierraItem))
      val miroWork = createMiroWork

      val result = mergeAndRedirectWork(
        Seq(
          sierraWork,
          miroWork
        ))

      val expectedMergedWork = sierraWork.copy(
        otherIdentifiers = sierraWork.otherIdentifiers ++ miroWork.identifiers,
        items = List(sierraItem.copy(
          agent = sierraItem.agent.copy(
            locations =
              sierraItem.agent.locations ++ miroWork.items.head.agent.locations
          )
        ))
      )

      val expectedRedirectedWork = UnidentifiedRedirectedWork(
        sourceIdentifier = miroWork.sourceIdentifier,
        version = miroWork.version,
        redirect = IdentifiableRedirect(sierraWork.sourceIdentifier)
      )

      result shouldBe List(expectedMergedWork, expectedRedirectedWork)
    }

    it("does not duplicate sierra-system-number when merging miro items") {
      val sierraItem =
        createIdentifiableItemWith(locations = List(createPhysicalLocation))
      val sierraWork = createSierraWorkWith(items = List(sierraItem))
      val libraryReferenceIdentifier = createMiroLibraryReferenceIdentifier
      val miroWork = createMiroWorkWith(
        otherIdentifiers =
          List(libraryReferenceIdentifier, sierraWork.sourceIdentifier))

      val result = mergeAndRedirectWork(
        Seq(
          sierraWork,
          miroWork
        ))

      val expectedMergedWork = sierraWork.copy(
        otherIdentifiers = sierraWork.otherIdentifiers ++ List(
          miroWork.sourceIdentifier,
          libraryReferenceIdentifier),
        items = List(sierraItem.copy(
          agent = sierraItem.agent.copy(
            locations =
              sierraItem.agent.locations ++ miroWork.items.head.agent.locations
          )
        ))
      )

      val expectedRedirectedWork = UnidentifiedRedirectedWork(
        sourceIdentifier = miroWork.sourceIdentifier,
        version = miroWork.version,
        redirect = IdentifiableRedirect(sierraWork.sourceIdentifier)
      )

      result shouldBe List(expectedMergedWork, expectedRedirectedWork)
    }

    it(
      "does not merge Miro DigitalLocation if the Sierra work already has a DigitalLocation") {
      val sierraItem =
        createIdentifiableItemWith(locations = List(createDigitalLocation))
      val sierraWork = createSierraWorkWith(items = List(sierraItem))
      val miroWork = createMiroWork

      val result = mergeAndRedirectWork(
        Seq(
          sierraWork,
          miroWork
        ))

      val expectedMergedWork = sierraWork.copy(
        otherIdentifiers = sierraWork.otherIdentifiers ++ miroWork.identifiers,
        items = List(sierraItem)
      )

      val expectedRedirectedWork = UnidentifiedRedirectedWork(
        sourceIdentifier = miroWork.sourceIdentifier,
        version = miroWork.version,
        redirect = IdentifiableRedirect(sierraWork.sourceIdentifier)
      )

      result shouldBe List(expectedMergedWork, expectedRedirectedWork)
    }
  }
}
