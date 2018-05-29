package uk.ac.wellcome.display.models.v2

import org.scalatest.Suite
import uk.ac.wellcome.display.models.DisplaySerialisationTestBase
import uk.ac.wellcome.models.work.internal.SourceIdentifier

trait DisplayV2SerialisationTestBase extends DisplaySerialisationTestBase {
  this: Suite =>
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
}
