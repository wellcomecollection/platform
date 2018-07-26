package uk.ac.wellcome.models.transformable.sierra

import org.scalatest.{Assertion, FunSpec, Matchers}
import uk.ac.wellcome.models.transformable.sierra.SierraRecordNumber._
import uk.ac.wellcome.models.transformable.sierra.test.utils.SierraUtil
import uk.ac.wellcome.test.utils.JsonTestUtil
import uk.ac.wellcome.utils.JsonUtil._

class SierraBibRecordTest extends FunSpec with Matchers with JsonTestUtil with SierraUtil {

  it("can cast a SierraBibRecord to JSON and back again") {
    val originalRecord = createSierraBibRecord

    val jsonString = toJson(originalRecord).get
    val parsedRecord = fromJson[SierraBibRecord](jsonString).get
    assertSierraBibRecordsAreEqual(parsedRecord, originalRecord)
  }

  private def assertSierraBibRecordsAreEqual(x: SierraBibRecord,
                                             y: SierraBibRecord): Assertion = {
    x.id shouldBe x.id
    assertJsonStringsAreEqual(x.data, y.data)
    x.modifiedDate shouldBe y.modifiedDate
  }
}
