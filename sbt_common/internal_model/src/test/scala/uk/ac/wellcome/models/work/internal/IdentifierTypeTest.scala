package uk.ac.wellcome.models.work.internal

import org.scalatest.{FunSpec, Matchers}

class IdentifierTypeTest extends FunSpec with Matchers {
  it("looks up an identifier type in the CSV") {
    IdentifierType(platformId = "MiroImageNumber") shouldBe IdentifierType(
      id = "miro-image-number",
      label = "Miro image number"
    )
  }
}
