package uk.ac.wellcome.display.models.v2

import org.scalatest.FunSpec
import uk.ac.wellcome.display.test.util.JsonMapperTestUtil
import uk.ac.wellcome.models.work.internal.{License_CCBY, License_CopyrightNotCleared}

class DisplayLicenseV2SerialisationTest
  extends FunSpec
    with DisplayV2SerialisationTestBase
    with JsonMapperTestUtil {

  it("includes the URL field on a license with a URL") {
    val displayLicense = DisplayLicenseV2(License_CCBY)

    val jsonString = objectMapper.writeValueAsString(displayLicense)

    assertJsonStringsAreEqual(
      jsonString,
      """
        |{
        |  "id": "cc-by",
        |  "label": "Attribution 4.0 International (CC BY 4.0)",
        |  "url": "http://creativecommons.org/licenses/by/4.0/",
        |  "type": "License"
        |}
      """.stripMargin
    )
  }

  it("omits the URL field on a license with a URL") {
    val displayLicense = DisplayLicenseV2(License_CopyrightNotCleared)

    val jsonString = objectMapper.writeValueAsString(displayLicense)

    assertJsonStringsAreEqual(
      jsonString,
      """
        |{
        |  "id": "copyright-not-cleared",
        |  "label": "Copyright not cleared",
        |  "type": "License"
        |}
      """.stripMargin
    )
  }
}
