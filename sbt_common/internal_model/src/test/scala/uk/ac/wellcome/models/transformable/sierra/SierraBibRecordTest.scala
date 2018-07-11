package uk.ac.wellcome.models.transformable.sierra

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.transformable.sierra.test.utils.SierraUtil
import uk.ac.wellcome.utils.JsonUtil._

class SierraBibRecordTest extends FunSpec with Matchers with SierraUtil {

  // Note: because SierraBibRecord includes a field of type 'Instant', this
  // isn't just provided by Circe's default case class encoder/decoders.
  //
  it("can cast a SierraBibRecord to JSON and back again") {
    val originalRecord = createSierraBibRecord

    val jsonString = toJson(originalRecord).get
    val parsedRecord = fromJson[SierraBibRecord](jsonString).get
    parsedRecord shouldEqual originalRecord
  }
}
