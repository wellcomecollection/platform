package uk.ac.wellcome.platform.archive.common.models
import org.scalatest.FunSpec
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.json.utils.JsonAssertions

class BagIdTest extends FunSpec with JsonAssertions {
  it("serialises space and external identifier as strings") {
    val bagId = BagId(
      space = Namespace("digitised"),
      externalIdentifier = ExternalIdentifier("b1234567x")
    )
    val expectedJson =
      s"""
         |{
         |  "space": "digitised",
         |  "externalIdentifier": "b1234567x"
         |}
       """.stripMargin
    assertJsonStringsAreEqual(toJson(bagId).get, expectedJson)
  }

}
