package uk.ac.wellcome.platform.archive.archivist.models

import java.util.zip.ZipFile

import uk.ac.wellcome.platform.archive.common.models.BagLocation

case class ArchiveJob(
  zipFile: ZipFile,
  bagLocation: BagLocation,
  config: BagItConfig,
  bagManifestLocations: List[BagManifestLocation]
) {
  def digestDelimiter = config.digestDelimiterRegexp
}
