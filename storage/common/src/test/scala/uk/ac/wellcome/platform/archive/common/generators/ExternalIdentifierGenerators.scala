package uk.ac.wellcome.platform.archive.common.generators

import uk.ac.wellcome.platform.archive.common.fixtures.RandomThings
import uk.ac.wellcome.platform.archive.common.models.bagit.ExternalIdentifier

trait ExternalIdentifierGenerators extends RandomThings {
  def createExternalIdentifier: ExternalIdentifier =
    ExternalIdentifier(randomAlphanumeric())
}
