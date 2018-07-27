package uk.ac.wellcome.platform.transformer.transformers.sierra

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.platform.transformer.transformers.ShouldNotTransformException
import uk.ac.wellcome.platform.transformer.utils.SierraDataUtil

class SierraTitleTest extends FunSpec with Matchers with SierraDataUtil {

  it("passes through the title from the bib record") {
    val title = "Tickling a tiny turtle in Tenerife"
    assertTitleIsCorrect(
      bibDataTitle = title,
      expectedTitle = title
    )
  }

  it("throws a ShouldNotTransform exception if bibData has no title") {
    val bibId = createSierraRecordNumberString
    val bibData = createSierraBibDataWith(title = None)
    val caught = intercept[ShouldNotTransformException] {
      transformer.getTitle(bibId = bibId, bibData = bibData)
    }
    caught.getMessage should be s"Sierra record $bibId has no title!"
  }

  val transformer = new Object with SierraTitle

  private def assertTitleIsCorrect(
    bibDataTitle: String,
    expectedTitle: String
  ) = {
    val bibData = createSierraBibDataWith(title = Some(bibDataTitle))
    transformer.getTitle(
      bibId = createSierraRecordNumberString,
      bibData = bibData) shouldBe expectedTitle
  }
}
