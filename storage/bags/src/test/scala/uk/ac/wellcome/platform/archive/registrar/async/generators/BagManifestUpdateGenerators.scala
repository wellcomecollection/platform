package uk.ac.wellcome.platform.archive.registrar.async.generators

import uk.ac.wellcome.platform.archive.common.fixtures.RandomThings
import uk.ac.wellcome.platform.archive.common.models.bagit.BagLocation
import uk.ac.wellcome.platform.archive.registrar.async.models.BagManifestUpdate

trait BagManifestUpdateGenerators extends RandomThings {
  def createBagManifestUpdateWith(
    archiveBagLocation: BagLocation,
    accessBagLocation: BagLocation): BagManifestUpdate =
    BagManifestUpdate(
      archiveRequestId = randomUUID,
      archiveBagLocation = archiveBagLocation,
      accessBagLocation = accessBagLocation
    )
}
