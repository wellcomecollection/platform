package uk.ac.wellcome.sierra_adapter.models

import io.circe.ParsingFailure
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.transformable.sierra.SierraItemRecord
import uk.ac.wellcome.sierra_adapter.models.test.utils.SierraRecordUtil

import scala.util.{Failure, Success}

class SierraRecordTest extends FunSpec with Matchers with SierraRecordUtil {

  describe("toItemRecord") {
    it("return a successful try for valid item json") {
      val id = "i12345"
      val bibId = "b54321"
      val data =
        s"""{
            "id": "$id",
            "bibIds" : ["$bibId"]
            }"""
      val sierraRecord = createSierraRecordWith(
        id = id,
        data = data
      )

      sierraRecord.toItemRecord shouldBe Success(
        SierraItemRecord(
          id = sierraRecord.id,
          data = sierraRecord.data,
          modifiedDate = sierraRecord.modifiedDate,
          bibIds = List(bibId),
          unlinkedBibIds = List()
        )
      )
    }

    it("return a failure for an invalid json") {
      val sierraRecord = createSierraRecordWith(data = "not a json string")

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

}
