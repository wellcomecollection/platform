package uk.ac.wellcome.platform.archive.registrar.http.models
import io.circe.generic.extras.JsonKey
import uk.ac.wellcome.platform.archive.common.models.DisplayStorageSpace
import uk.ac.wellcome.platform.archive.registrar.common.models._

case class DisplayBag(
  id: String,
  space: DisplayStorageSpace,
  info: DisplayBagInfo,
  manifest: DisplayBagManifest,
  accessLocation: DisplayLocation,
  createdDate: String,

  @JsonKey("type")
  ontologyType: String = "Bag"
              )

object DisplayBag {
  def apply(storageManifest: StorageManifest): DisplayBag = DisplayBag(
    storageManifest.id.toString,
    DisplayStorageSpace(storageManifest.id.space.underlying),
    DisplayBagInfo(storageManifest.id.externalIdentifier.underlying),
    DisplayBagManifest(storageManifest.manifest),
    DisplayLocation(storageManifest.accessLocation),
    storageManifest.createdDate.toString
  )
}
