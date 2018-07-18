package uk.ac.wellcome.models.transformable.sierra

import org.scalatest.{FunSpec, Matchers}

class SierraRecordNumberTest extends FunSpec with Matchers {
  it("allows creating from a 7-digit ID") {
    SierraRecordNumber("1234567")
  }

  it("does not allow creating a SierraBibRecord with an ID that isn't 7 digits") {
    assertCreatingWithIdFails("1234567890")
  }

  it("does not allow creating a SierraBibRecord with an ID that isn't numeric") {
    assertCreatingWithIdFails("NaN")
  }

  describe("withCheckDigit") {
    it("adds a 'b' prefix to bibs") {
      SierraRecordNumber("1024364").withCheckDigit(SierraRecordTypes.bibs) shouldBe "b10243641"
    }

    it("adds an 'i' prefix to bibs") {
      SierraRecordNumber("1024364").withCheckDigit(SierraRecordTypes.items) shouldBe "i10243641"
    }

    it("can add an 'x' check digit") {
      SierraRecordNumber("1840974").withCheckDigit(SierraRecordTypes.items) shouldBe "i1840974x"
    }
  }

  describe("increments") {
    it("increments an ID correctly") {
      SierraRecordNumber("1000000").increment shouldBe SierraRecordNumber("1000001")
    }
  }

  private def assertCreatingWithIdFails(s: String) = {
    val caught = intercept[IllegalArgumentException] {
      SierraRecordNumber(s)
    }

    caught.getMessage shouldBe s"requirement failed: Not a 7-digit Sierra record number: $s"
  }
}
