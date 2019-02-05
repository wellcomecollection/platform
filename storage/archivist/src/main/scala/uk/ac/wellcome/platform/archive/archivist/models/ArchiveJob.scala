package uk.ac.wellcome.platform.archive.archivist.models

import java.util.zip.ZipFile

import uk.ac.wellcome.platform.archive.common.models.bagit.{
  BagItemPath,
  BagLocation,
  ExternalIdentifier
}

/** Used internally by an archivist flow.
  *
  * @param externalIdentifier  The external identifier for the bag
  *
  * @param zipFile              The downloaded ZIP file on the local disk.
  * @param bagLocation          Where to upload the new bag to.
  * @param config               Information about what meta files we expect to see in the
  *                             bag, and what formats we expect them to be in.
  * @param bagManifestLocations A list of manifest locations inside the bag.
  */
case class ArchiveJob(
  externalIdentifier: ExternalIdentifier,
  zipFile: ZipFile,
  bagLocation: BagLocation,
  config: BagItConfig,
  bagManifestLocations: List[BagItemPath]
)
