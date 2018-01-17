package uk.ac.wellcome.platform.api.utils

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.{IdentifierSchemes, SourceIdentifier, Work}

class ApiJsonUtilTest extends FunSpec with Matchers {

  it("should round-trip an empty list back to an empty list") {
    val jsonString =
      """{"canonicalId": null, "title": "A doodle of a dog", "subjects": [] }"""
    val parsedWork = ApiJsonUtil.fromJson[Work](jsonString).get

    parsedWork.canonicalId shouldBe None
    parsedWork.subjects shouldBe Nil
  }
}
