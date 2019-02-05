package uk.ac.wellcome.platform.archive.registrar.async.models

import java.util.UUID

import uk.ac.wellcome.platform.archive.common.models.bagit.BagLocation

/** Represents an update to a "bag manifest" -- that is, the collection of
  * bags in S3 that we want to associate with a storage manifest.
  *
  */
case class BagManifestUpdate(
  archiveRequestId: UUID,
  archiveBagLocation: BagLocation,
  accessBagLocation: BagLocation
)
