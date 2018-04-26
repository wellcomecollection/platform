package uk.ac.wellcome.transformer.transformers.sierra

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.test.utils.SierraData
import uk.ac.wellcome.transformer.source.SierraBibData
import uk.ac.wellcome.transformer.source.sierra.{Language => SierraLanguageField}
import uk.ac.wellcome.work_model.Language

class SierraLanguageTest extends FunSpec with Matchers with SierraData {

  val transformer = new SierraLanguage {}

  it("ignores records which don't have a lang field") {
    val bibData = SierraBibData(
      id = "1000001",
      lang = None
    )

    transformer.getLanguage(bibData = bibData) shouldBe None
  }

  it("picks up the language from the lang field") {
    val bibData = SierraBibData(
      id = "2000002",
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
