package uk.ac.wellcome.platform.archive.archivist.models

import java.util.zip.ZipFile

import uk.ac.wellcome.platform.archive.common.models.bagit.{
  BagItemPath,
  BagLocation,
  ExternalIdentifier
}

/** Used internally by an archivist flow.
  *
  * @param externalIdentifier   The external identifier for the bag
  * @param zipFile              The downloaded ZIP file on the local disk.
  * @param bagRootPathInZip     Where the bag root can be found in the ZIP file.
  * @param bagUploadLocation    Where to upload the new bag to.
  * @param tagManifestLocation  Where the tag manifest is in the ZIP file.
  * @param bagManifestLocations A list of manifest locations inside the bag.
  * @param config               Information about what meta files we expect to see in the
  *                              bag, and what formats we expect them to be in.
  */
case class ArchiveJob(externalIdentifier: ExternalIdentifier,
                      zipFile: ZipFile,
                      bagRootPathInZip: Option[String] = None,
                      bagUploadLocation: BagLocation,
                      tagManifestLocation: BagItemPath,
                      bagManifestLocations: List[BagItemPath],
                      config: BagItConfig)
