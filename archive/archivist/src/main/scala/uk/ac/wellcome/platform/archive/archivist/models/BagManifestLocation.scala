package uk.ac.wellcome.platform.archive.archivist.models

import uk.ac.wellcome.platform.archive.common.models.BagPath
import uk.ac.wellcome.storage.ObjectLocation

case class BagManifestLocation(path: BagPath, name: String) {
  def toObjectLocation = ObjectLocation(path.value, name)
}

object BagManifestLocation {
  def create(config: BagItConfig, bagPath: BagPath): List[BagManifestLocation] =
    config.digestNames.map(BagManifestLocation(bagPath, _))
}

