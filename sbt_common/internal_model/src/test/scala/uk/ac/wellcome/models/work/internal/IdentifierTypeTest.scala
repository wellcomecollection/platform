package uk.ac.wellcome.models.work.internal

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.exceptions.InternalModelException

class IdentifierTypeTest extends FunSpec with Matchers {
  it("looks up an identifier type in the CSV") {
    IdentifierType("miro-image-number") shouldBe IdentifierType(
      id = "miro-image-number",
      label = "Miro image number"
    )
  }

  it("throws an error if looking up a non-existent identifier type") {
    val caught = intercept[InternalModelException] {
      IdentifierType(platformId = "DoesNotExist")
    }
    caught.e.getMessage shouldBe "Unrecognised identifier type: [DoesNotExist]"
  }
}
