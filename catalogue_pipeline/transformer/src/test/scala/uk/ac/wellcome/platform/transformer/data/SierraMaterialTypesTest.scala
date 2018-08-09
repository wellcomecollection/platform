package uk.ac.wellcome.platform.transformer.data

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.work.internal.WorkType
import uk.ac.wellcome.platform.transformer.exceptions.TransformerException

class SierraMaterialTypesTest extends FunSpec with Matchers {
  it("looks up a WorkType by code") {
    SierraMaterialTypes.fromCode("a") shouldBe WorkType(id = "a", label = "books")
  }

  it("throws an exception if passed an unrecognised code") {
    val caught = intercept[TransformerException] {
      SierraMaterialTypes.fromCode("?")
    }
    caught.e.getMessage shouldBe "Unrecognised work type code: ?"
  }

  it("throws an exception if passed an empty string") {
    val caught = intercept[TransformerException] {
      SierraMaterialTypes.fromCode("")
    }
    caught.e.getMessage shouldBe "Work type code is not a single character: <<>>"
  }

  it("throws an exception if passed a code which is more than a single char") {
    val caught = intercept[TransformerException] {
      SierraMaterialTypes.fromCode("XXX")
    }
    caught.e.getMessage shouldBe "Work type code is not a single character: <<XXX>>"
  }
}
