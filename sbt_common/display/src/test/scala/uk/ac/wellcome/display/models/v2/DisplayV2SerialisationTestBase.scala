package uk.ac.wellcome.display.models.v2

import org.scalatest.Suite
import uk.ac.wellcome.display.models.DisplaySerialisationTestBase
import uk.ac.wellcome.models.work.internal.SourceIdentifier

trait DisplayV2SerialisationTestBase extends DisplaySerialisationTestBase { this: Suite =>
  def identifier(identifier: SourceIdentifier) =
    s"""{
      "type": "Identifier",
      "identifierScheme": "${identifier.identifierType.id}",
      "value": "${identifier.value}"
    }"""
}
