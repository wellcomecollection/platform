package uk.ac.wellcome.platform.archive.common.generators

import uk.ac.wellcome.platform.archive.common.models.BagId

trait BagIdGenerators extends NamespaceGenerators {
  def createBagId: BagId = BagId(
    space = createNamespace,
    externalIdentifier = randomExternalIdentifier
  )
}
