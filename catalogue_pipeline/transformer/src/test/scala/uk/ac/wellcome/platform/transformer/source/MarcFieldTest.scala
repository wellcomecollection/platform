package uk.ac.wellcome.platform.transformer.source

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.utils.JsonUtil._
import uk.ac.wellcome.test.utils.JsonTestUtil

class MarcFieldTest extends FunSpec with Matchers with JsonTestUtil {

  it("converts a long-form VarField to JSON") {
    val varField = VarField(
      fieldTag = "y",
      marcTag = "007",
      indicator1 = " ",
      indicator2 = " ",
      subfields = List()
    )

    assertJsonStringsAreEqual(
      toJson(varField).get,
      s"""{
        "fieldTag": "${varField.fieldTag}",
        "marcTag": "${varField.marcTag.get}",
        "content": null,
        "ind1": "${varField.indicator1.get}",
        "ind2": "${varField.indicator2.get}",
        "subfields": []
      }"""
    )
  }

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
      fieldTag = "n",
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

  it("converts a short-form VarField to JSON") {
    val varField = VarField(
      fieldTag = "b",
      content = "A dallying dance of ducks"
    )

    assertJsonStringsAreEqual(
      toJson(varField).get,
      s"""{
        "fieldTag": "${varField.fieldTag}",
        "content": "${varField.content.get}",
        "marcTag": null,
        "ind1": null,
        "ind2": null,
        "subfields": []
      }"""
    )
  }

  it("reads a JSON string as a short-form VarField") {
    val jsonString = s"""{
      "fieldTag": "c",
      "content": "Enjoying an event with enormous eagles"
    }"""

    val expectedVarField = VarField(
      fieldTag = "c",
      content = "Enjoying an event with enormous eagles"
    )

    fromJson[VarField](jsonString).get shouldBe expectedVarField
  }
}
