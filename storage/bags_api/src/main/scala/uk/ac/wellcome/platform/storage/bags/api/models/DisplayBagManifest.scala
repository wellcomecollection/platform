package uk.ac.wellcome.platform.storage.bags.api.models

import io.circe.generic.extras.JsonKey
import uk.ac.wellcome.platform.archive.registrar.common.models.FileManifest

case class DisplayBagManifest(
  checksumAlgorithm: String,
  files: List[DisplayFileDigest],
  @JsonKey("type")
  ontologyType: String = "BagManifest"
)
object DisplayBagManifest {
  def apply(fileManifest: FileManifest): DisplayBagManifest =
    DisplayBagManifest(
      fileManifest.checksumAlgorithm.value,
      fileManifest.files.map(DisplayFileDigest.apply))
}
