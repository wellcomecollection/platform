package uk.ac.wellcome.platform.api.utils

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.{IdentifierSchemes, SourceIdentifier, Work}

class ApiJsonUtilTest extends FunSpec with Matchers {
  it("should not include fields where the value is empty or None") {
    val identifier = SourceIdentifier(
      identifierScheme = IdentifierSchemes.miroImageNumber,
      value = "value"
    )

    val work = Work(
      sourceIdentifier = identifier,
      identifiers = List(identifier),
      title = "A haiku about a heron"
    )
    val jsonString = ApiJsonUtil.toJson(work).get

    jsonString.contains(""""accessStatus":null""") should be(false)
    jsonString.contains(""""identifiers":[]""") should be(false)
  }

  it("should round-trip an empty list back to an empty list") {
    val jsonString = """{"accessStatus": [], "title": "A doodle of a dog"}"""
    val parsedWork = ApiJsonUtil.fromJson[Work](jsonString).get
    val extrapolatedString = ApiJsonUtil.toJson(parsedWork).get

    jsonString.contains(""""accessStatus": []""") shouldBe true
  }
}
