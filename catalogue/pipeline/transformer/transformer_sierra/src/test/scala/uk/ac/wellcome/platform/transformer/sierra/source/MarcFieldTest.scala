package uk.ac.wellcome.platform.transformer.sierra.source

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.json.utils.JsonAssertions

class MarcFieldTest extends FunSpec with Matchers with JsonAssertions {

  // These tests are intended to check that we can parse a VarField in
  // both forms sent by Sierra.

  it("reads a JSON string as a long-form VarField") {
    val jsonString = s"""{
      "fieldTag": "n",
      "marcTag": "008",
      "ind1": " ",
      "ind2": " ",
      "subfields": [
        {
          "tag": "a",
          "content": "An armada of armadillos"
        },
        {
          "tag": "b",
          "content": "A bonanza of bears"
        },
        {
          "tag": "c",
          "content": "A cacophany of crocodiles"
        }
      ]
    }"""

    val expectedVarField = VarField(
      marcTag = "008",
      indicator1 = " ",
      indicator2 = " ",
      subfields = List(
        MarcSubfield(tag = "a", content = "An armada of armadillos"),
        MarcSubfield(tag = "b", content = "A bonanza of bears"),
        MarcSubfield(tag = "c", content = "A cacophany of crocodiles")
      )
    )

    val varField = fromJson[VarField](jsonString).get
    varField shouldBe expectedVarField
  }

  it("reads a JSON string as a short-form VarField") {
    val jsonString = s"""{
      "fieldTag": "c",
      "content": "Enjoying an event with enormous eagles"
    }"""

    val expectedVarField = VarField(
      content = "Enjoying an event with enormous eagles"
    )

    fromJson[VarField](jsonString).get shouldBe expectedVarField
  }
}
