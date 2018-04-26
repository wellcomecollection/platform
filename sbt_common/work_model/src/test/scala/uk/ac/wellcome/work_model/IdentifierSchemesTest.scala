package uk.ac.wellcome.work_model

class IdentifierSchemesTest extends FunSpec with Matchers with JsonTestUtil {

  it("serialises a known identifier scheme to JSON") {
    val data: Map[String, IdentifierSchemes.IdentifierScheme] =
      Map("scheme" -> IdentifierSchemes.miroImageNumber)
    val expectedJsonString = """{"scheme": "miro-image-number"}"""
    val actualJsonString = toJson(data).get

    assertJsonStringsAreEqual(expectedJsonString, actualJsonString)
  }

  it("deserialises a known identifier scheme from JSON") {
    val jsonString = """{"scheme": "sierra-identifier"}"""
    val expectedData = Map("scheme" -> IdentifierSchemes.sierraIdentifier)
    val actualData =
      fromJson[Map[String, IdentifierSchemes.IdentifierScheme]](jsonString).get

    actualData shouldBe expectedData
  }

  it("throws an error if asked to deserialise an unknown identifier scheme") {
    val jsonString = """{"scheme": "unknown-identifier-scheme"}"""

    val caught = intercept[Exception] {
      fromJson[Map[String, IdentifierSchemes.IdentifierScheme]](jsonString)
    }

    caught.getMessage shouldEqual "unknown-identifier-scheme is not a valid identifierScheme"
  }
}
