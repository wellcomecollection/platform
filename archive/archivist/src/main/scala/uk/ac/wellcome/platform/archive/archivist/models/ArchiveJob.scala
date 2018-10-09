package uk.ac.wellcome.platform.archive.archivist.models

import java.util.zip.ZipFile

import uk.ac.wellcome.platform.archive.common.models.BagLocation

/** Used internally by an archivist flow.
  *
  * @param zipFile The downloaded ZIP file on the local disk.
  * @param bagLocation Where to upload the new bag to.
  * @param config Information about what meta files we expect to see in the
  *               bag, and what formats we expect them to be in.
  * @param bagManifestLocations A list of manifest locations inside the bag.
  */
case class ArchiveJob(
  zipFile: ZipFile,
  bagLocation: BagLocation,
  config: BagItConfig,
  bagManifestLocations: List[BagManifestLocation]
) {
  def digestDelimiter = config.digestDelimiterRegexp
}
