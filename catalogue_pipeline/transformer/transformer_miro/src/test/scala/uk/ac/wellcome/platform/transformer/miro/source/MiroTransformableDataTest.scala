package uk.ac.wellcome.platform.transformer.miro.source

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.platform.transformer.miro.generators.MiroTransformableGenerators

class MiroTransformableDataTest
    extends FunSpec
    with Matchers
    with MiroTransformableGenerators {

  // This is based on failures we've seen in the pipeline.
  // Examples: L0068740, V0014729.
  it("parses a JSON string with nulls in the image_keywords_unauth field") {
    val jsonString =
      buildJSONForWork(extraData = """
        |  "image_keywords_unauth": [null, null]
      """.stripMargin)

    fromJson[MiroTransformableData](jsonString).isSuccess shouldBe true
  }

  // This is based on bugs from data in the pipeline.
  it("corrects the entities in Adêle Mongrédien's name") {
    val jsonString = buildJSONForWork(
      extraData = """
        |  "image_creator": ["Ad\u00c3\u00aale Mongr\u00c3\u00a9dien"]
      """.stripMargin)

    MiroTransformableData.create(jsonString).creator shouldBe Some(
      List(Some("Adêle Mongrédien")))
  }
}
