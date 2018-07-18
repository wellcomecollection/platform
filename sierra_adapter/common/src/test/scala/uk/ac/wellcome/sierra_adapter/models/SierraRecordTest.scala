package uk.ac.wellcome.sierra_adapter.models

import io.circe.ParsingFailure
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.transformable.sierra.SierraItemRecord

import scala.util.{Failure, Success}

class SierraRecordTest extends FunSpec with Matchers {
  val modifiedDate = "2001-01-01T01:01:01Z"
  describe("toItemRecord") {
    it("return a successful try for valid item json") {
      val id = "1234567"
      val bibId = "b54321"
      val data =
        s"""{
            "id": "$id",
            "bibIds" : ["$bibId"]
            }"""
      val sierraRecord =
        SierraRecord(id = id, data = data, modifiedDate = modifiedDate)

      sierraRecord.toItemRecord shouldBe Success(
        SierraItemRecord(
          id = id,
          data = data,
          modifiedDate = modifiedDate,
          bibIds = List(bibId)))
    }

    it("return a failure for an invalid json") {
      val sierraRecord = SierraRecord(
        id = "1234567",
        data = "not a json string",
        modifiedDate = modifiedDate)

      val triedItemRecord = sierraRecord.toItemRecord
      triedItemRecord shouldBe a[Failure[_]]
      triedItemRecord.failed.get shouldBe a[ParsingFailure]
    }

    it("return a failure if the json is valid but contains no bibIds") {
      val sierraRecord =
        SierraRecord(id = "1234567", data = "{}", modifiedDate = modifiedDate)

      val triedItemRecord = sierraRecord.toItemRecord
      triedItemRecord shouldBe a[Failure[_]]
      triedItemRecord.failed.get shouldBe a[IllegalArgumentException]
    }

    it("return a failure if bibIds is a list of non strings") {
      val sierraRecord = SierraRecord(
        id = "1234567",
        data = """{"bibIds":[1,2,3]}""",
        modifiedDate = modifiedDate)

      val triedItemRecord = sierraRecord.toItemRecord
      triedItemRecord shouldBe a[Failure[_]]
      triedItemRecord.failed.get shouldBe a[IllegalArgumentException]
    }

    it("return a failure if bibIds is not a list") {
      val sierraRecord = SierraRecord(
        id = "1234567",
        data = """{"bibIds":"blah"}""",
        modifiedDate = modifiedDate)

      val triedItemRecord = sierraRecord.toItemRecord
      triedItemRecord shouldBe a[Failure[_]]
      triedItemRecord.failed.get shouldBe a[IllegalArgumentException]
    }
  }

  it("does not allow creating a SierraRecord with an ID that isn't 7 digits") {
    val caught = intercept[IllegalArgumentException] {
      SierraRecord(
        id = "123456789",
        data = """{"title": "A jumping jaguar in a jungle of junipers"}""",
        modifiedDate = modifiedDate
      )
    }

    caught.getMessage shouldBe "requirement failed: Not a 7-digit Sierra record number: 123456789"
  }
}
