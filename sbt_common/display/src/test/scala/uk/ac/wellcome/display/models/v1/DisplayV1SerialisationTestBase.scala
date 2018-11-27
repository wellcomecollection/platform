package uk.ac.wellcome.display.models.v1

import org.scalatest.Suite
import uk.ac.wellcome.display.models.DisplaySerialisationTestBase
import uk.ac.wellcome.models.work.internal.{
  License,
  LocationType,
  SourceIdentifier
}

trait DisplayV1SerialisationTestBase extends DisplaySerialisationTestBase {
  this: Suite =>

  def license(license: License) =
    s"""{
      "label": "${license.label}",
      "licenseType": "${license.id.toUpperCase}",
      ${optionalString("url", license.url)}
      "type": "${license.ontologyType}"
    }"""

  def identifier(identifier: SourceIdentifier) =
    s"""{
      "type": "Identifier",
      "identifierScheme": "${identifier.identifierType.id}",
      "value": "${identifier.value}"
    }"""

  def locationType(locType: LocationType): String =
    s"""
       |"${locType.id}"
     """.stripMargin
}
