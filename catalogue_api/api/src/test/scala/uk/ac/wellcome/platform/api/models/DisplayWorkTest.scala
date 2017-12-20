package uk.ac.wellcome.platform.api.models

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models._

class DisplayWorkTest extends FunSpec with Matchers {

  it("should parse a JSON body without any items") {
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
    displayWork.items shouldBe List()
  }
}
