package uk.ac.wellcome.display.models.v1

import org.scalatest.Suite
import uk.ac.wellcome.display.models.DisplaySerialisationTestBase
import uk.ac.wellcome.models.work.internal.SourceIdentifier

trait DisplayV1SerialisationTestBase extends DisplaySerialisationTestBase { this: Suite =>
  def identifier(identifier: SourceIdentifier) =
    s"""{
      "type": "Identifier",
      "identifierScheme": "${identifier.identifierType.id}",
      "value": "${identifier.value}"
    }"""
}
