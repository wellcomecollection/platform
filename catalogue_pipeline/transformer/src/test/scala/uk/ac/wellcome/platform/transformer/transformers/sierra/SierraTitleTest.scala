package uk.ac.wellcome.platform.transformer.transformers.sierra

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.platform.transformer.source.SierraBibData
import uk.ac.wellcome.test.utils.SierraData

class SierraTitleTest extends FunSpec with Matchers with SierraData {

  it("passes through the title from the bib record") {
    val title = "Tickling a tiny turtle in Tenerife"
    assertTitleIsCorrect(
      bibDataTitle = title,
      expectedTitle = title
    )
  }

  val transformer = new Object with SierraTitle

  private def assertTitleIsCorrect(
    bibDataTitle: String,
    expectedTitle: String
  ) = {

    val bibData = SierraBibData(
      id = "b1234567",
      title = Some(bibDataTitle),
      deleted = false,
      suppressed = false
    )

    transformer.getTitle(bibData = bibData) shouldBe Some(expectedTitle)
  }
}
