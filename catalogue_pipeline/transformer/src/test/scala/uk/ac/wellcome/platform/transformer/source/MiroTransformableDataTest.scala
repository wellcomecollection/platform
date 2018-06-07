package uk.ac.wellcome.platform.transformer.source

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.utils.JsonUtil._

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

  // This is based on failures we've seen in the pipeline.
  // Example: B0009887
  it("parses a JSON string with a list in the image_source_code field") {
    val jsonString =
      """
        |{
        |   "image_source_code":["GUS","TO-DELETE"]
        |}
      """.stripMargin

    val result = MiroTransformableData.create(jsonString)
    result.sourceCode shouldBe Some("GUS")
  }

  // And since we're fudging with the image_source_code field, check
  // the happy path still works as well.
  it("parses a JSON string with a string in the image_source_code field") {
    val jsonString =
      """
        |{
        |   "image_source_code": "CGC"
        |}
      """.stripMargin

    val result = MiroTransformableData.create(jsonString)
    result.sourceCode shouldBe Some("CGC")
  }
}
