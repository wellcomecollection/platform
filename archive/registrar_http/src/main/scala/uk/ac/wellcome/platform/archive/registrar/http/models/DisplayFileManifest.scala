package uk.ac.wellcome.platform.archive.registrar.http.models
import io.circe.generic.extras.JsonKey
import uk.ac.wellcome.platform.archive.registrar.common.models.FileManifest

case class DisplayFileManifest(
  checksumAlgorithm: String,
  files: List[DisplayFileDigest],
                                @JsonKey("type")
ontologyType: String = "FileManifest"
                              )
object DisplayFileManifest {
  def apply(fileManifest: FileManifest): DisplayFileManifest = DisplayFileManifest(fileManifest.checksumAlgorithm.value, fileManifest.files.map(DisplayFileDigest.apply))
}