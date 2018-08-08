package uk.ac.wellcome.models.transformable.sierra

import org.scalatest.{FunSpec, Matchers}

class SierraRecordNumberTest extends FunSpec with Matchers {
  describe("withCheckDigit") {
    it("handles a bib example from the Sierra docs") {
      SierraBibNumber("1024364").withCheckDigit shouldBe "b10243641"
    }

    it("handles an item example from the catalogue") {
      SierraItemNumber("1828888").withCheckDigit shouldBe "i18288881"
    }

    it("adds an 'x' where necessary") {
      SierraItemNumber("1840974").withCheckDigit shouldBe "i1840974x"
    }
  }

  describe("withoutCheckDigit") {
    it("handles a bib example") {
      SierraBibNumber("1024364").withoutCheckDigit shouldBe "1024364"
    }

    it("handles an item example") {
      SierraItemNumber("1828888").withoutCheckDigit shouldBe "1828888"
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

  private def assertStringFailsValidation(recordNumber: String) = {
    val caught = intercept[IllegalArgumentException] {
      SierraBibNumber(recordNumber)
    }

    caught.getMessage shouldEqual s"requirement failed: Not a 7-digit Sierra record number: $recordNumber"
  }
}
