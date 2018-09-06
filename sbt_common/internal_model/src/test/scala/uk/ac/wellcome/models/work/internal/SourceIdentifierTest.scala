package uk.ac.wellcome.models.work.internal

import org.scalatest.{FunSpec, Matchers}

class SourceIdentifierTest extends FunSpec with Matchers {
  it("has the correct toString value") {
    val sourceIdentifier = SourceIdentifier(
      identifierType = IdentifierType("miro-image-number"),
      value = "A0001234",
      ontologyType = "Work"
    )

    sourceIdentifier.toString shouldBe "miro-image-number/A0001234"
  }
}
