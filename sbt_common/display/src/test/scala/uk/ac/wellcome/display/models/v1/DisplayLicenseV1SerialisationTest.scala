package uk.ac.wellcome.display.models.v1

import org.scalatest.FunSpec
import uk.ac.wellcome.display.test.util.JsonMapperTestUtil
import uk.ac.wellcome.models.work.internal.{License_CCBY, License_CopyrightNotCleared}

class DisplayLicenseV1SerialisationTest
  extends FunSpec
  with DisplayV1SerialisationTestBase
  with JsonMapperTestUtil {

  it("includes the URL field on a license with a URL") {
    val displayLicense = DisplayLicenseV1(License_CCBY)

    val jsonString = objectMapper.writeValueAsString(displayLicense)

    assertJsonStringsAreEqual(
      jsonString,
      """
        |{
        |  "label": "Attribution 4.0 International (CC BY 4.0)",
        |  "licenseType": "CC-BY",
        |  "type": "License",
        |  "url": "http://creativecommons.org/licenses/by/4.0/"
        |}
      """.stripMargin
    )
  }

  it("omits the URL field on a license with a URL") {
    val displayLicense = DisplayLicenseV1(License_CopyrightNotCleared)

    val jsonString = objectMapper.writeValueAsString(displayLicense)

    assertJsonStringsAreEqual(
      jsonString,
      """
        |{
        |  "label": "Copyright not cleared",
        |  "licenseType": "copyright-not-cleared",
        |  "type": "License"
        |}
      """.stripMargin
    )
  }
}
