package uk.ac.wellcome.platform.transformer.transformers.sierra

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.transformable.sierra.test.utils.SierraData
import uk.ac.wellcome.platform.transformer.source.SierraBibData
import uk.ac.wellcome.platform.transformer.transformers.miro.ShouldNotTransformException

class SierraTitleTest extends FunSpec with Matchers with SierraData {

  it("passes through the title from the bib record") {
    val title = "Tickling a tiny turtle in Tenerife"
    assertTitleIsCorrect(
      bibDataTitle = title,
      expectedTitle = title
    )
  }

  it("throws a ShouldNotTransform exception if bibData has no title") {
    val bibData = SierraBibData(
      id = "b1234567",
      title = None,
      deleted = false,
      suppressed = false
    )
    intercept[ShouldNotTransformException] {
      transformer.getTitle(bibData = bibData)
    }
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

    transformer.getTitle(bibData = bibData) shouldBe expectedTitle
  }
}
