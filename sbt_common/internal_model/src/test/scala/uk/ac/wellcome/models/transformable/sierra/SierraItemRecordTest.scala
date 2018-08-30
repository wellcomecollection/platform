package uk.ac.wellcome.models.transformable.sierra

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.models.transformable.sierra.test.utils.SierraGenerators

import java.time.Instant

class SierraItemRecordTest extends FunSpec with Matchers with SierraGenerators {

  it("can cast a SierraItemRecord to JSON and back again") {
    val originalRecord = createSierraItemRecord

    val jsonString = toJson(originalRecord).get
    val parsedRecord = fromJson[SierraItemRecord](jsonString).get
    parsedRecord shouldEqual originalRecord
  }

  it("creates a SierraItemRecord from valid item JSON") {
    val itemId = createSierraRecordNumberString
    val bibId = createSierraBibNumber
    val data = s"""
       |{
       |  "id": "$itemId",
       |  "bibIds" : ["$bibId"]
       |}
       |""".stripMargin

    val result = SierraItemRecord(
      id = itemId,
      data = data,
      modifiedDate = Instant.now
    )

    result.id shouldBe SierraItemNumber(itemId)
    result.data shouldBe data
    result.bibIds shouldBe List(bibId)
  }

  it("throws an exception for invalid JSON") {
    assertCreatingFromDataFails(
      data = "not a json string",
      expectedMessage = "expected null got n (line 1, column 1)"
    )
  }

  it("throws an exception for valid JSON that doesn't contain bibIds") {
    assertCreatingFromDataFails(
      data = "{}",
      expectedMessage =
        "Attempt to decode value on failed cursor: DownField(bibIds)"
    )
  }

  it("throws an exception when bibIds is not a list of strings") {
    assertCreatingFromDataFails(
      data = """{"bibIds":[1,2,3]}""",
      expectedMessage = "String: DownArray,DownField(bibIds)"
    )
  }

  it("throws an exception when bibIds is not a list") {
    assertCreatingFromDataFails(
      data = """{"bibIds":"blah"}""",
      expectedMessage = "CanBuildFrom for A: DownField(bibIds)"
    )
  }

  private def assertCreatingFromDataFails(
    data: String,
    expectedMessage: String
  ) {
    val caught = intercept[IllegalArgumentException] {
      SierraItemRecord(
        id = createSierraRecordNumberString,
        data = data,
        modifiedDate = Instant.now
      )
    }

    caught.getMessage shouldBe s"Error parsing bibIds from JSON <<$data>> (uk.ac.wellcome.json.exceptions.JsonDecodingError: $expectedMessage)"
  }
}
