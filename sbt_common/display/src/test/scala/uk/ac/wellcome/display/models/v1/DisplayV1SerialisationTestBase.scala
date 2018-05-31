package uk.ac.wellcome.display.models.v1

import org.scalatest.Suite
import uk.ac.wellcome.display.models.DisplaySerialisationTestBase
import uk.ac.wellcome.models.work.internal.{License, SourceIdentifier}

trait DisplayV1SerialisationTestBase extends DisplaySerialisationTestBase {
  this: Suite =>

  def license(license: License) =
    s"""{
      "label": "${license.label}",
      "licenseType": "${license.id.toUpperCase}",
      "type": "${license.ontologyType}",
      "url": "${license.url}"
    }"""

  def identifier(identifier: SourceIdentifier) =
    s"""{
      "type": "Identifier",
      "identifierScheme": "${identifier.identifierType.id}",
      "value": "${identifier.value}"
    }"""
}
