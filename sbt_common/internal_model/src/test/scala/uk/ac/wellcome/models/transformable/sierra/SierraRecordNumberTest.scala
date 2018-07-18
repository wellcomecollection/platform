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

  private def assertCreatingWithIdFails(s: String) = {
    val caught = intercept[IllegalArgumentException] {
      SierraRecordNumber(s)
    }

    caught.getMessage shouldBe s"requirement failed: Not a 7-digit Sierra record number: $s"
  }
}
