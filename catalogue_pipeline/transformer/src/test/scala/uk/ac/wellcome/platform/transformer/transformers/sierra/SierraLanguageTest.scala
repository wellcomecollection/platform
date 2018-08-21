package uk.ac.wellcome.platform.transformer.transformers.sierra

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.work.internal.Language
import uk.ac.wellcome.platform.transformer.source.sierra.{
  Language => SierraLanguageField
}
import uk.ac.wellcome.platform.transformer.utils.SierraDataGenerators

class SierraLanguageTest extends FunSpec with Matchers with SierraDataGenerators {

  val transformer = new SierraLanguage {}

  it("ignores records which don't have a lang field") {
    val bibData = createSierraBibDataWith(lang = None)
    transformer.getLanguage(bibData = bibData) shouldBe None
  }

  it("picks up the language from the lang field") {
    val bibData = createSierraBibDataWith(
      lang = Some(
        SierraLanguageField(
          code = "eng",
          name = "English"
        ))
    )

    transformer.getLanguage(bibData = bibData) shouldBe Some(
      Language(
        id = "eng",
        label = "English"
      ))
  }
}
