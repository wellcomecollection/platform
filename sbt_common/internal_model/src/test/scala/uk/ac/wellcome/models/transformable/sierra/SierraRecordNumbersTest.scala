package uk.ac.wellcome.models.transformable.sierra

import org.scalatest.{Assertion, FunSpec, Matchers}
import org.scalatest.prop.TableDrivenPropertyChecks._

class SierraRecordNumbersTest extends FunSpec with Matchers {

  describe("addCheckDigit") {
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

          SierraRecordNumbers.addCheckDigit(
            sierraId,
            recordType = sierraRecordType) shouldBe expectedId
      }
    }

    it("throws an error if passed a Sierra ID which is non-numeric") {
      assertThrowsAssertionError(
        () => SierraRecordNumbers.addCheckDigit(
          "abcdefg",
          recordType = SierraRecordTypes.bibs),
        expectedMessage = "addCheckDigit() can only be used with 7-digit record numbers; not abcdefg"
      )
    }

    it("throws an error if passed a Sierra ID which is too short") {
      assertThrowsAssertionError(
        () => SierraRecordNumbers.addCheckDigit(
          "123",
          recordType = SierraRecordTypes.bibs),
        expectedMessage = "addCheckDigit() can only be used with 7-digit record numbers; not 123"
      )
    }

    it("throws an error if passed a Sierra ID which is too long") {
      assertThrowsAssertionError(
        () => SierraRecordNumbers.addCheckDigit(
          "12345678",
          recordType = SierraRecordTypes.bibs),
        expectedMessage = "addCheckDigit() can only be used with 7-digit record numbers; not 12345678"
      )
    }
  }

  describe("assertValidRecordNumber") {
    it("accepts a valid 7-digit record number") {
      SierraRecordNumbers.assertValidRecordNumber("1234567")
    }

    it("accepts a valid 8-digit record number") {
      SierraRecordNumbers.assertValidRecordNumber("12345678")
    }

    it("rejects an invalid string") {
      assertThrowsAssertionError(
        () => SierraRecordNumbers.assertValidRecordNumber("NaN"),
        expectedMessage = "Not a valid Sierra record number: NaN"
      )
    }
  }

  describe("assertIs7DigitRecordNumber") {
    it("accepts a valid record number") {
      SierraRecordNumbers.assertIs7DigitRecordNumber("1234567")
    }

    it("rejects an invalid record number") {
      assertThrowsAssertionError(
        () => SierraRecordNumbers.assertIs7DigitRecordNumber("NaN"),
        expectedMessage = "Not a 7-digit Sierra record number: NaN"
      )
    }
  }

  describe("assertIs8DigitRecordNumber") {
    it("accepts a valid record number") {
      SierraRecordNumbers.assertIs8DigitRecordNumber("12345678")
    }

    it("rejects an invalid record number") {
      assertThrowsAssertionError(
        () => SierraRecordNumbers.assertIs8DigitRecordNumber("NaN"),
        expectedMessage = "Not an 8-digit Sierra record number: NaN"
      )
    }
  }

  describe("increment") {
    it("rejects 8-digit record numbers") {
      assertThrowsAssertionError(
        () => SierraRecordNumbers.increment("12345678"),
        expectedMessage =
          "increment() can only be used with 7-digit record numbers; not 12345678"
      )
    }

    it("rejects bad inputs") {
      assertThrowsAssertionError(
        () => SierraRecordNumbers.increment("NaN"),
        expectedMessage =
          "increment() can only be used with 7-digit record numbers; not NaN"
      )
    }

    it("increments a 7-digit record number") {
      SierraRecordNumbers.increment("1000001") shouldBe "1000002"
    }

    it("increments a 7-digit record number that wraps around") {
      SierraRecordNumbers.increment("1000009") shouldBe "1000010"
    }
  }

  private def assertThrowsAssertionError(f: => () => Unit,
                                         expectedMessage: String): Assertion = {
    val caught = intercept[AssertionError] { f() }
    caught.getMessage shouldBe s"assertion failed: $expectedMessage"
  }
}
