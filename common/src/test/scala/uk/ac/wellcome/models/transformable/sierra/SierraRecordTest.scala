package uk.ac.wellcome.models.transformable.sierra

import java.time.Instant

import com.amazonaws.services.kinesis.model.InvalidArgumentException
import io.circe.ParsingFailure
import org.scalatest.{FunSpec, Matchers}

import scala.util.{Failure, Success}

class SierraRecordTest extends FunSpec with Matchers {
  val modifiedDate = Instant.now()
  describe("toItemRecord") {
    it("return a successful try for valid item json") {
      val id = "i12345"
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
        id = "i12345",
        data = "not a json string",
        modifiedDate = modifiedDate)

      val triedItemRecord = sierraRecord.toItemRecord
      triedItemRecord shouldBe a[Failure[_]]
      triedItemRecord.failed.get shouldBe a[ParsingFailure]
    }

    it("return a failure if the json is valid but contains no bibIds") {
      val sierraRecord =
        SierraRecord(id = "i12345", data = "{}", modifiedDate = modifiedDate)

      val triedItemRecord = sierraRecord.toItemRecord
      triedItemRecord shouldBe a[Failure[_]]
      triedItemRecord.failed.get shouldBe a[InvalidArgumentException]
    }

    it("return a failure if bibIds is a list of non strings") {
      val sierraRecord = SierraRecord(
        id = "i12345",
        data = """{"bibIds":[1,2,3]}""",
        modifiedDate = modifiedDate)

      val triedItemRecord = sierraRecord.toItemRecord
      triedItemRecord shouldBe a[Failure[_]]
      triedItemRecord.failed.get shouldBe a[InvalidArgumentException]
    }

    it("return a failure if bibIds is not a list") {
      val sierraRecord = SierraRecord(
        id = "i12345",
        data = """{"bibIds":"blah"}""",
        modifiedDate = modifiedDate)

      val triedItemRecord = sierraRecord.toItemRecord
      triedItemRecord shouldBe a[Failure[_]]
      triedItemRecord.failed.get shouldBe a[InvalidArgumentException]
    }
  }

}