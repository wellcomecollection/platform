package uk.ac.wellcome.platform.transformer.source

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.utils.JsonUtil._

class MiroTransformableDataTest extends FunSpec with Matchers {

  // This is a simplified example of a failure we saw in the pipeline.
  // Examples: L0068740, V0014729.
  it("parses a JSON string with nulls in the image_keywords_unauth field") {
    val jsonString =
      """
        |{
        |    "image_artwork_date": "1578",
        |    "image_award": null,
        |    "image_award_date": null,
        |    "image_cleared": "Y",
        |    "image_copyright_cleared": "Y",
        |    "image_creator": null,
        |    "image_credit_line": null,
        |    "image_image_desc": "De morbis contagiosis libri septem / [Julien Le Paulmier].",
        |    "image_image_desc_academic": null,
        |    "image_innopac_id": "12062789",
        |    "image_keywords": null,
        |    "image_keywords_unauth": [
        |        null
        |    ],
        |    "image_lc_genre": null,
        |    "image_library_ref_department": [
        |        "EPB"
        |    ],
        |    "image_library_ref_id": [
        |        "4856/B/1"
        |    ],
        |    "image_phys_format": null,
        |    "image_source_code": "WEL",
        |    "image_supp_lettering": null,
        |    "image_tech_file_size": [
        |        "99854740"
        |    ],
        |    "image_title": "De morbis contagiosis libri septem",
        |    "image_use_restrictions": "CC-BY"
        |}
        |
      """.stripMargin

    fromJson[MiroTransformableData](jsonString).isSuccess shouldBe true
  }
}
