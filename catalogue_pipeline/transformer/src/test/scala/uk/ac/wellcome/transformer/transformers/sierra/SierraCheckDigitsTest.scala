package uk.ac.wellcome.transformer.transformers.sierra

import org.scalatest.{FunSpec, Matchers}

class SierraCheckDigitsTest extends FunSpec with Matchers {

  it("throws an error if passed a Sierra ID which is non-numeric") {
    val caught = intercept[RuntimeException] {
      transformer.addCheckDigit("abcdefg", recordType = SierraRecordTypes.bibs)
    }

    caught.getMessage shouldEqual "Expected 7-digit numeric ID, got abcdefg"
  }

  it("throws an error if passed a Sierra ID which is too short") {
    val caught = intercept[RuntimeException] {
      transformer.addCheckDigit("123", recordType = SierraRecordTypes.bibs)
    }

    caught.getMessage shouldEqual "Expected 7-digit numeric ID, got 123"
  }

  it("throws an error if passed a Sierra ID which is too long") {
    val caught = intercept[RuntimeException] {
      transformer.addCheckDigit("12345678", recordType = SierraRecordTypes.bibs)
    }

    caught.getMessage shouldEqual "Expected 7-digit numeric ID, got 12345678"
  }

  val transformer = new SierraCheckDigits {}
}
