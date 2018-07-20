package uk.ac.wellcome.platform.transformer.transformers.sierra

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.work.internal.{IdentifierType, SourceIdentifier}
import uk.ac.wellcome.platform.transformer.utils.SierraDataUtil

class SierraIdentifiersTest extends FunSpec with Matchers with SierraDataUtil {

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
    val bibData = createSierraBibDataWith(id = bibDataId)
    transformer.getOtherIdentifiers(bibData = bibData) shouldBe expectedIdentifiers
  }
}
