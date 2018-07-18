package uk.ac.wellcome.sierra_adapter.models

import io.circe.ParsingFailure
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.transformable.sierra.{SierraItemRecord, SierraRecordNumber}
import uk.ac.wellcome.sierra_adapter.test.util.SierraRecordUtil

import scala.util.{Failure, Success}

class SierraRecordTest extends FunSpec with Matchers with SierraRecordUtil {

  describe("toItemRecord") {
    it("return a successful try for valid item json") {
      val id = createSierraRecordNumberString
      val bibId = createSierraRecordNumberString
      val data =
        s"""{
            "id": "$id",
            "bibIds" : ["$bibId"]
            }"""
      val sierraRecord = createSierraRecordWith(id = id, data = data)

      sierraRecord.toItemRecord shouldBe Success(
        SierraItemRecord(
          id = SierraRecordNumber(id),
          data = data,
          modifiedDate = sierraRecord.modifiedDate,
          bibIds = List(SierraRecordNumber(bibId))))
    }

    it("return a failure for an invalid json") {
      val sierraRecord = createSierraRecordWith(
        data = "not a json string"
      )

      val triedItemRecord = sierraRecord.toItemRecord
      triedItemRecord shouldBe a[Failure[_]]
      triedItemRecord.failed.get shouldBe a[ParsingFailure]
    }

    it("return a failure if the json is valid but contains no bibIds") {
      val sierraRecord = createSierraRecordWith(data = "{}")

      val triedItemRecord = sierraRecord.toItemRecord
      triedItemRecord shouldBe a[Failure[_]]
      triedItemRecord.failed.get shouldBe a[IllegalArgumentException]
    }

    it("return a failure if bibIds is a list of non strings") {
      val sierraRecord = createSierraRecordWith(
        data = """{"bibIds":[1,2,3]}"""
      )

      val triedItemRecord = sierraRecord.toItemRecord
      triedItemRecord shouldBe a[Failure[_]]
      triedItemRecord.failed.get shouldBe a[IllegalArgumentException]
    }

    it("return a failure if bibIds is not a list") {
      val sierraRecord = createSierraRecordWith(
        data = """{"bibIds":"blah"}"""
      )

      val triedItemRecord = sierraRecord.toItemRecord
      triedItemRecord shouldBe a[Failure[_]]
      triedItemRecord.failed.get shouldBe a[IllegalArgumentException]
    }
  }

  it("does not allow creating a SierraRecord with an ID that isn't 7 digits") {
    val caught = intercept[IllegalArgumentException] {
      createSierraRecordWith(id = "123456789")
    }

    caught.getMessage shouldBe "requirement failed: Not a 7-digit Sierra record number: 123456789"
  }
}
