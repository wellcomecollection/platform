package uk.ac.wellcome.platform.transformer.transformers.sierra

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.work.internal.{IdentifierType, SourceIdentifier}
import uk.ac.wellcome.platform.transformer.utils.SierraDataUtil

class SierraIdentifiersTest extends FunSpec with Matchers with SierraDataUtil {

  it("passes through the main identifier from the bib record") {
    val id = createSierraRecordNumberString

    val bibData = createSierraBibDataWith(id = id)

    val expectedIdentifiers = List(
      SourceIdentifier(
        identifierType = IdentifierType("sierra-identifier"),
        ontologyType = "Work",
        value = id
      )
    )

    transformer.getOtherIdentifiers(bibData = bibData) shouldBe expectedIdentifiers
  }

  val transformer = new Object with SierraIdentifiers
}
