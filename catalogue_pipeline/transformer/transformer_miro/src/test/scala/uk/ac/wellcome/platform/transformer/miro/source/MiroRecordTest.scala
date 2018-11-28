package uk.ac.wellcome.platform.transformer.miro.source

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.json.JsonUtil._

class MiroRecordTest extends FunSpec with Matchers {

  // This is based on failures we've seen in the pipeline.
  // Examples: L0068740, V0014729.
  it("parses a JSON string with nulls in the image_keywords_unauth field") {
    val jsonString =
      """
        |{
        |  "image_no_calc": "L0068740",
        |  "image_keywords_unauth": [null, null]
        |}
        |
      """.stripMargin

    fromJson[MiroRecord](jsonString).isSuccess shouldBe true
  }

  // This is based on bugs from data in the pipeline.
  it("corrects the entities in Adêle Mongrédien's name") {
    val jsonString =
      """
        |{
        |  "image_no_calc": "A0000001",
        |  "image_creator": ["Ad\u00c3\u00aale Mongr\u00c3\u00a9dien"]
        |}
      """.stripMargin

    MiroRecord.create(jsonString).creator shouldBe Some(
      List(Some("Adêle Mongrédien")))
  }
}
