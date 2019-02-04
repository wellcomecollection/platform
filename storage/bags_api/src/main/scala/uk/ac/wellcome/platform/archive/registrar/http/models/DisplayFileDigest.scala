package uk.ac.wellcome.platform.archive.registrar.http.models
import io.circe.generic.extras.JsonKey
import uk.ac.wellcome.platform.archive.common.models.bagit.BagDigestFile

case class DisplayFileDigest(checksum: String,
                             path: String,
                             @JsonKey("type")
                             ontologyType: String = "File")
object DisplayFileDigest {
  def apply(bagDigestFile: BagDigestFile): DisplayFileDigest =
    DisplayFileDigest(bagDigestFile.checksum.value, bagDigestFile.path.toString)
}
