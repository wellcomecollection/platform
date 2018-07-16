package uk.ac.wellcome.platform.transformer.transformers.sierra

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.work.internal.{IdentifierType, SourceIdentifier}
import uk.ac.wellcome.platform.transformer.source.SierraBibData

class SierraIdentifiersTest extends FunSpec with Matchers {

  it("passes through the main identifier from the bib record") {
    assertIdentifiersAreCorrect(
      bibDataId = "1782863",
      expectedIdentifiers = List(
        SourceIdentifier(
          identifierType = IdentifierType("sierra-identifier"),
          ontologyType = "Work",
          value = "1782863"
        )
      )
    )
  }

  val transformer = new Object with SierraIdentifiers

  private def assertIdentifiersAreCorrect(
    bibDataId: String,
    expectedIdentifiers: List[SourceIdentifier]
  ) = {

    val bibData = SierraBibData(
      id = bibDataId,
      title = Some("An imprint of insects on the inside of an igloo"),
      deleted = false,
      suppressed = false
    )

    transformer.getOtherIdentifiers(bibData = bibData) shouldBe expectedIdentifiers
  }
}
