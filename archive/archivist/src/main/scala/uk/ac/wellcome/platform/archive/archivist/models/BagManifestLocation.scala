package uk.ac.wellcome.platform.archive.archivist.models
import uk.ac.wellcome.platform.archive.common.models.EntryPath

case class BagManifestLocation(name: String){
  def toEntryPath = EntryPath(name)
}

object BagManifestLocation {
  def create(config: BagItConfig): List[BagManifestLocation] =
    config.digestNames.map(BagManifestLocation(_))
}
