package uk.ac.wellcome.platform.transformer.miro.source

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.json.JsonUtil._

class MiroTransformableDataTest extends FunSpec with Matchers {

  // This is based on failures we've seen in the pipeline.
  // Examples: L0068740, V0014729.
  it("parses a JSON string with nulls in the image_keywords_unauth field") {
    val jsonString =
      """
        |{
        |    "image_keywords_unauth": [null, null]
        |}
        |
      """.stripMargin

    fromJson[MiroTransformableData](jsonString).isSuccess shouldBe true
  }
}
