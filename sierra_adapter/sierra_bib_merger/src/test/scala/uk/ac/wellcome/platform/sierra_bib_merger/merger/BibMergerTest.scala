package uk.ac.wellcome.platform.sierra_bib_merger.merger

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.transformable.SierraTransformable
import uk.ac.wellcome.models.transformable.sierra.test.utils.SierraUtil

class BibMergerTest extends FunSpec with Matchers with SierraUtil {

  describe("merging with a SierraBibRecord") {
    it("should merge data from a bibRecord when empty") {
      val record = createSierraBibRecord
      val originalRecord = SierraTransformable(sourceId = record.id)

      val newRecord = BibMerger.mergeBibRecord(originalRecord, record)
      newRecord.maybeBibData.get shouldEqual record
    }

    it("should only merge bib records with matching ids") {
      val record = createSierraBibRecordWith(id = "1")
      val originalRecord = SierraTransformable(sourceId = "2")

      val caught = intercept[RuntimeException] {
        BibMerger.mergeBibRecord(originalRecord, record)
      }
      caught.getMessage shouldEqual "Non-matching bib ids 1 != 2"
    }

    it(
      "should return the unmerged record when merging bib records with stale data") {
      val record = createSierraBibRecordWith(
        data = "<<old data>>",
        modifiedDate = olderDate
      )

      val sierraTransformable = SierraTransformable(
        sourceId = record.id,
        maybeBibData = Some(
          createSierraBibRecordWith(
            id = record.id,
            data = "<<new data>>",
            modifiedDate = newerDate
          )
        )
      )

      val result = BibMerger.mergeBibRecord(sierraTransformable, record)
      result shouldBe sierraTransformable
    }

    it("should update bibData when merging bib records with newer data") {
      val record = createSierraBibRecordWith(
        data = "<<new data>>",
        modifiedDate = newerDate
      )

      val sierraTransformable = SierraTransformable(
        sourceId = record.id,
        maybeBibData = Some(
          createSierraBibRecordWith(
            id = record.id,
            data = "<<old data>>",
            modifiedDate = olderDate
          )
        )
      )

      val result = BibMerger.mergeBibRecord(sierraTransformable, record)
      result.maybeBibData.get shouldBe record
    }
  }
}
