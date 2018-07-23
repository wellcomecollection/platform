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
    val bibData = createSierraBibDataWith(title = None)
    intercept[ShouldNotTransformException] {
      transformer.getTitle(bibData = bibData)
    }
  }

  val transformer = new Object with SierraTitle

  private def assertTitleIsCorrect(
    bibDataTitle: String,
    expectedTitle: String
  ) = {
    val bibData = createSierraBibDataWith(title = Some(bibDataTitle))
    transformer.getTitle(bibData = bibData) shouldBe expectedTitle
  }
}
