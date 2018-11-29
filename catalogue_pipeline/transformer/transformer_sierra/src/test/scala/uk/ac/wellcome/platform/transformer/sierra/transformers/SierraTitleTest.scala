package uk.ac.wellcome.platform.transformer.sierra.transformers

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.platform.transformer.sierra.exceptions.ShouldNotTransformException
import uk.ac.wellcome.platform.transformer.sierra.generators.SierraDataGenerators

class SierraTitleTest extends FunSpec with Matchers with SierraDataGenerators {

  it("passes through the title from the bib record") {
    val title = "Tickling a tiny turtle in Tenerife"
    assertTitleIsCorrect(
      bibDataTitle = title,
      expectedTitle = title
    )
  }

  it("throws a ShouldNotTransform exception if bibData has no title") {
    val bibData = createSierraBibDataWith(title = None)
    val caught = intercept[ShouldNotTransformException] {
      transformer.getTitle(bibData = bibData)
    }
    caught.getMessage shouldBe "Sierra record has no title!"
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
