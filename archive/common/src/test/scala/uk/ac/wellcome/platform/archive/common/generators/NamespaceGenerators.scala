package uk.ac.wellcome.platform.archive.common.generators

import uk.ac.wellcome.platform.archive.common.fixtures.RandomThings
import uk.ac.wellcome.platform.archive.common.models.Namespace

trait NamespaceGenerators extends RandomThings {
  def createNamespace: Namespace = Namespace(randomAlphanumeric())
}
