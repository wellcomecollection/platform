package uk.ac.wellcome.models.work.internal

import org.scalatest.{FunSpec, Matchers}

class IdentifierTypeTest extends FunSpec with Matchers {
  it("looks up an identifier type in the CSV") {
    IdentifierType("miro-image-number") shouldBe IdentifierType(
      id = "miro-image-number",
      label = "Miro image number"
    )
  }

  it("throws an error if looking up a non-existent identifier type") {
    val caught = intercept[IllegalArgumentException] {
      IdentifierType(platformId = "DoesNotExist")
    }
    caught.getMessage shouldBe "Unrecognised identifier type: [DoesNotExist]"
  }
}
