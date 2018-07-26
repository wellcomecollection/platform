package uk.ac.wellcome.models.transformable.sierra

import org.scalatest.{FunSpec, Matchers}
import org.scalatest.prop.TableDrivenPropertyChecks._

class SierraRecordNumberTest extends FunSpec with Matchers {

  describe("withCheckDigit") {
    val testCases = Table(
      // Example from the Sierra docs
      ("1024364", "bibs", "b10243641"),
      // Examples taken from catalogue records
      ("1828888", "bibs", "b18288881"),
      ("1828888", "items", "i18288881"),
      // Example from a catalogue record with an "x" check digit
      ("1840974", "items", "i1840974x")
    )

    it("correctly handles all of our test cases") {
      forAll(testCases) {
        (sierraId: String, recordType: String, expectedId: String) =>
          val sierraRecordType = recordType match {
            case "bibs"  => SierraRecordTypes.bibs
            case "items" => SierraRecordTypes.items
          }

          SierraRecordNumber(sierraId).withCheckDigit(sierraRecordType) shouldBe expectedId
      }
    }
  }

  it("throws an error if passed a Sierra ID which is non-numeric") {
    assertStringFailsValidation("abcdefg")
  }

  it("throws an error if passed a Sierra ID which is too short") {
    assertStringFailsValidation("123")
  }

  it("throws an error if passed a Sierra ID which is too long") {
    assertStringFailsValidation("12345678")
  }

  private def assertStringFailsValidation(s: String) = {
    val caught = intercept[IllegalArgumentException] {
      SierraRecordNumber(s)
    }

    caught.getMessage shouldEqual s"requirement failed: Not a 7-digit Sierra record number: $s"
  }
}
