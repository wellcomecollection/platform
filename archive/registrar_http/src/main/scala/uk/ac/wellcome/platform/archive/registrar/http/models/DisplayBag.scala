package uk.ac.wellcome.platform.archive.registrar.http.models
import io.circe.generic.extras.JsonKey
import uk.ac.wellcome.platform.archive.common.models.DisplayStorageSpace
import uk.ac.wellcome.platform.archive.registrar.common.models.{BagDigestFile, FileManifest, StorageManifest}

case class DisplayBag(
  id: String,
  space: DisplayStorageSpace,
  info: DisplayBagInfo,
  manifest: DisplayFileManifest,
  createdDate: String,

  @JsonKey("type")
  ontologyType: String = "Bag"
              )

object DisplayBag {
  def apply(storageManifest: StorageManifest): DisplayBag = DisplayBag(
    storageManifest.id.toString,
    DisplayStorageSpace(storageManifest.id.space.underlying),
    DisplayBagInfo(storageManifest.id.externalIdentifier.underlying),
    DisplayFileManifest(storageManifest.manifest),
    storageManifest.createdDate.toString
  )
}

case class DisplayBagInfo(
  externalIdentifier: String,
  @JsonKey("type")
  ontologyType: String = "BagInfo"
                         )

case class DisplayFileManifest(
  checksumAlgorithm: String,
  files: List[DisplayFileDigest],
                                @JsonKey("type")
ontologyType: String = "FileManifest"
                              )

object DisplayFileManifest {
  def apply(fileManifest: FileManifest): DisplayFileManifest = DisplayFileManifest(fileManifest.checksumAlgorithm.value, fileManifest.files.map(DisplayFileDigest.apply))
}

case class DisplayFileDigest(checksum: String,
                             path: String,
  @JsonKey("type")
                             ontologyType: String = "File")

object DisplayFileDigest {
  def apply(bagDigestFile: BagDigestFile): DisplayFileDigest = DisplayFileDigest(bagDigestFile.checksum.value, bagDigestFile.path.value)
}