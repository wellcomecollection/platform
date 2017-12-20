package uk.ac.wellcome.platform.api.models

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models._

class DisplayWorkTest extends FunSpec with Matchers {

  it("correctly parses a JSON body without any items") {
    val title = "An irritating imp is immune from items"
    val document = s"""{
      "title": "$title",
      "sourceIdentifier": {
        "identifierScheme": "sierra-system-number",
        "value": "b1234567"
      },
      "identifiers": [
        {
          "identifierScheme": "sierra-system-number",
          "value": "b1234567"
        }
      ],
      "canonicalId": "abcdef12",
      "type": "Work"
    }""".stripMargin

    val displayWork = DisplayWork.jsonToDisplayWork(
      document = document,
      includes = WorksIncludes(items = true)
    )
    displayWork.items shouldBe Some(List())
  }

  it("correctly parses items on a work") {
    val document = s"""{
      "title": "Inside an irate igloo",
      "sourceIdentifier": {
        "identifierScheme": "miro-image-number",
        "value": "V0000001"
      },
      "canonicalId": "b4heraz7",
      "items": [
        {
          "canonicalId": "c3a599u5",
          "sourceIdentifier": {
            "identifierScheme": "miro-image-number",
            "value": "V0000001"
          },
          "identifiers": [
            {
              "identifierScheme": "miro-image-number",
              "value": "M0000001"
            }
          ],
          "type": "Item"
        }
      ],
      "type":"Work"
    }"""
    val displayWork = DisplayWork.jsonToDisplayWork(
      document = document,
      includes = WorksIncludes(items = true)
    )
    displayWork.items shouldBe Some(DisplayItem(
      id = "c3a599u5",
      identifiers = Some(List(
        DisplayIdentifier(
          identifierScheme = IdentifierSchemes.miroImageNumber,
          value = "M0000001"
        )
      ))
    ))
  }

  it("throws a RuntimeException if you try to parse invalid JSON") {
    intercept[RuntimeException] {
      DisplayWork.jsonToDisplayWork(
        document = "<xml><is pretty=nifty></xml>",
        includes = WorksIncludes()
      )
    }
  }
}
