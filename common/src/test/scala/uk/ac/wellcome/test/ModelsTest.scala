package uk.ac.wellcome.platform.common

import org.scalatest.{FunSpec, Matchers}

import uk.ac.wellcome.models.Work
import uk.ac.wellcome.utils.JsonUtil

class WorkTest extends FunSpec with Matchers {
  it("should have an LD type 'Work' when serialised to JSON") {
    val work = Work(
      identifiers = List(),
      label = "A book about a blue whale"
    )
    val jsonString = JsonUtil.toJson(work).get

    jsonString.contains("""type":"Work"""") should be(true)
  }
}

class JsonUtilTest extends FunSpec with Matchers {
  it("should not include fields where the value is empty or None") {
    val work = Work(
      identifiers = List(),
      label = "A haiku about a heron"
    )
    val jsonString = JsonUtil.toJson(work).get

    jsonString.contains(""""accessStatus":null""") should be(false)
    jsonString.contains(""""identifiers":[]""") should be(false)
  }

  it("should round-trip an empty list back to an empty list") {
    val jsonString = """{"accessStatus": [], "label": "A doodle of a dog"}"""
    val parsedWork = JsonUtil.fromJson[Work](jsonString).get
    val extrapolatedString = JsonUtil.toJson(parsedWork).get

    jsonString.contains(""""accessStatus": []""") shouldBe true
  }
}
