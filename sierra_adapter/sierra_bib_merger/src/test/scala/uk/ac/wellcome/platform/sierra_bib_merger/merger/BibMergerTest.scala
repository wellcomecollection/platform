package uk.ac.wellcome.platform.sierra_bib_merger.merger

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.transformable.SierraTransformable
import uk.ac.wellcome.models.transformable.sierra.test.utils.SierraUtil

class BibMergerTest extends FunSpec with Matchers with SierraUtil {

  describe("merging with a SierraBibRecord") {
    it("should merge data from a bibRecord when empty") {
      val bibRecord = createSierraBibRecord
      val originalTransformable = SierraTransformable(sierraId = bibRecord.id)

      val newTransformable =
        BibMerger.mergeBibRecord(originalTransformable, bibRecord)
      newTransformable.maybeBibData.get shouldEqual bibRecord
    }

    it("only merges bib records with matching ids") {
      val bibRecord = createSierraBibRecord
      val altId = createSierraRecordNumber
      val originalTransformable = SierraTransformable(sierraId = altId)

      val caught = intercept[RuntimeException] {
        BibMerger.mergeBibRecord(originalTransformable, bibRecord)
      }
      caught.getMessage shouldEqual s"Non-matching bib ids ${bibRecord.id} != $altId"
    }

    it("returns the unmerged record when merging bib records with stale data") {
      val bibRecord = createSierraBibRecordWith(modifiedDate = olderDate)

      val sierraTransformable = SierraTransformable(
        sierraId = bibRecord.id,
        maybeBibData = Some(
          bibRecord.copy(modifiedDate = newerDate)
        )
      )

      val result = BibMerger.mergeBibRecord(sierraTransformable, bibRecord)
      result shouldBe sierraTransformable
    }

    it("should update bibData when merging bib records with newer data") {
      val bibRecord = createSierraBibRecordWith(modifiedDate = newerDate)

      val sierraTransformable = SierraTransformable(
        sierraId = bibRecord.id,
        maybeBibData = Some(
          bibRecord.copy(modifiedDate = olderDate)
        )
      )

      val result = BibMerger.mergeBibRecord(sierraTransformable, bibRecord)
      result.maybeBibData.get shouldBe bibRecord
    }
  }
}
