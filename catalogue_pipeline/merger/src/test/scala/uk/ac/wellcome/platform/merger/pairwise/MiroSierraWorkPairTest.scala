package uk.ac.wellcome.platform.merger.pairwise

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.work.internal.{SourceIdentifier, UnidentifiedWork}
import uk.ac.wellcome.models.work.test.util.WorksGenerators

class MiroSierraWorkPairTest extends FunSpec with Matchers with WorksGenerators {
  describe("copying identifiers") {
    it("copies identifiers from the Miro work to the Sierra work") {
      val miroWork = createMiroWorkWith(
        otherIdentifiers = List(
          createSourceIdentifierWith(identifierType = "miro-image-number"),
          createSourceIdentifierWith(identifierType = "miro-library-reference")
        )
      )

      val sierraWork = createSierraWorkWith(
        otherIdentifiers = List(
          createSourceIdentifierWith(identifierType = "sierra-identifier")
        )
      )

      val result = MiroSierraWorkPair.mergeAndRedirectWork(
        miroWork = miroWork,
        sierraWork = sierraWork
      )
      result.get.mergedWork.identifiers shouldBe sierraWork.identifiers ++ miroWork.identifiers
    }
  }

  private def createSierraWorkWith(otherIdentifiers: List[SourceIdentifier]): UnidentifiedWork =
    createUnidentifiedWorkWith(
      sourceIdentifier = createSourceIdentifierWith(identifierType = "sierra-system-number"),
      otherIdentifiers = otherIdentifiers
    )

  private def createMiroWorkWith(otherIdentifiers: List[SourceIdentifier]): UnidentifiedWork =
    createUnidentifiedWorkWith(
      sourceIdentifier = createSourceIdentifierWith(identifierType = "miro-image-number"),
      otherIdentifiers = otherIdentifiers
    )
}
