package uk.ac.wellcome.platform.common

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.{IdentifierSchemes, SourceIdentifier, Work}
import uk.ac.wellcome.utils.JsonUtil

class JsonUtilTest extends FunSpec with Matchers {
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
    val jsonString = JsonUtil.toJson(work).get

    jsonString.contains(""""accessStatus":null""") should be(false)
    jsonString.contains(""""identifiers":[]""") should be(false)
  }

  it("should round-trip an empty list back to an empty list") {
    val jsonString = """{"accessStatus": [], "title": "A doodle of a dog"}"""
    val parsedWork = JsonUtil.fromJson[Work](jsonString).get
    val extrapolatedString = JsonUtil.toJson(parsedWork).get

    jsonString.contains(""""accessStatus": []""") shouldBe true
  }
}
