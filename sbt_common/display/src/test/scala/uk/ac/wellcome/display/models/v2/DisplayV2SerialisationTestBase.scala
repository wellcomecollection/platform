package uk.ac.wellcome.display.models.v2

import org.scalatest.Suite
import uk.ac.wellcome.display.models.DisplaySerialisationTestBase
import uk.ac.wellcome.models.work.internal.{
  License,
  LocationType,
  SourceIdentifier
}

trait DisplayV2SerialisationTestBase extends DisplaySerialisationTestBase {
  this: Suite =>

  def license(license: License) =
    s"""{
      "id": "${license.id}",
      "label": "${license.label}",
      ${optionalString("url", license.url)}
      "type": "${license.ontologyType}"
    }"""

  def identifier(identifier: SourceIdentifier) =
    s"""{
      "type": "Identifier",
      "identifierType": {
        "id": "${identifier.identifierType.id}",
        "label": "${identifier.identifierType.label}",
        "type": "${identifier.identifierType.ontologyType}"
      },
      "value": "${identifier.value}"
    }"""

  def locationType(locType: LocationType): String =
    s"""{
       |  "id": "${locType.id}",
       |  "label": "${locType.label}",
       |  "type": "LocationType"
       |}
     """.stripMargin

}
