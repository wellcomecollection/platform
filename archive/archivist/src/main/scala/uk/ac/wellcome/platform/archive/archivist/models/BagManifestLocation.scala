package uk.ac.wellcome.platform.archive.archivist.models
import uk.ac.wellcome.platform.archive.common.models.{BagFilePath}

case class BagManifestLocation(name: String) {
  def toBagFilePath = BagFilePath(name)
}

object BagManifestLocation {
  def create(config: BagItConfig): List[BagManifestLocation] =
    config.digestNames.map(BagManifestLocation(_))
}
