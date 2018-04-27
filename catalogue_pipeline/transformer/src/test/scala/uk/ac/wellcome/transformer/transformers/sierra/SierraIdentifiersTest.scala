package uk.ac.wellcome.transformer.transformers.sierra

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.work.internal.{
  IdentifierSchemes,
  SourceIdentifier
}
import uk.ac.wellcome.test.utils.SierraData
import uk.ac.wellcome.transformer.source.SierraBibData

class SierraIdentifiersTest extends FunSpec with Matchers with SierraData {

  it("passes through the main identifier from the bib record") {
    assertIdentifiersAreCorrect(
      bibDataId = "1782863",
      expectedIdentifiers = List(
        SourceIdentifier(
          identifierScheme = IdentifierSchemes.sierraSystemNumber,
          ontologyType = "Work",
          value = "b17828636"
        ),
        SourceIdentifier(
          identifierScheme = IdentifierSchemes.sierraIdentifier,
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

    transformer.getIdentifiers(bibData = bibData) shouldBe expectedIdentifiers
  }
}
