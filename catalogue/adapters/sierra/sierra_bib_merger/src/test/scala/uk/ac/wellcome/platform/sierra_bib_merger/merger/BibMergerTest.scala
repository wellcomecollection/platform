package uk.ac.wellcome.platform.sierra_bib_merger.merger

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.transformable.SierraTransformable
import uk.ac.wellcome.models.transformable.sierra.test.utils.SierraGenerators

class BibMergerTest extends FunSpec with Matchers with SierraGenerators {

  describe("merging with a SierraBibRecord") {
    it("merges data from a bibRecord when empty") {
      val bibRecord = createSierraBibRecord
      val transformable = createSierraTransformableWith(
        sierraId = bibRecord.id,
        maybeBibRecord = None
      )

      val newTransformable = BibMerger.mergeBibRecord(transformable, bibRecord)
      newTransformable.maybeBibRecord.get shouldEqual bibRecord
    }

    it("only merges bib records with matching ids") {
      val bibRecord = createSierraBibRecord
      val transformable = createSierraTransformable

      val caught = intercept[RuntimeException] {
        BibMerger.mergeBibRecord(transformable, bibRecord)
      }
      caught.getMessage shouldEqual s"Non-matching bib ids ${bibRecord.id} != ${transformable.sierraId}"
    }

    it(
      "returns the unmerged transformable when merging bib records with stale data") {
      val oldBibRecord = createSierraBibRecordWith(
        modifiedDate = olderDate
      )

      val newBibRecord = createSierraBibRecordWith(
        id = oldBibRecord.id,
        modifiedDate = newerDate
      )

      val transformable = SierraTransformable(
        bibRecord = newBibRecord
      )

      val result = BibMerger.mergeBibRecord(transformable, oldBibRecord)
      result shouldBe transformable
    }

    it("updates bibData when merging bib records with newer data") {
      val newBibRecord = createSierraBibRecordWith(
        modifiedDate = newerDate
      )

      val oldBibRecord = createSierraBibRecordWith(
        id = newBibRecord.id,
        modifiedDate = olderDate
      )

      val transformable = SierraTransformable(
        bibRecord = oldBibRecord
      )

      val result = BibMerger.mergeBibRecord(transformable, newBibRecord)
      result.maybeBibRecord.get shouldBe newBibRecord
    }
  }
}
