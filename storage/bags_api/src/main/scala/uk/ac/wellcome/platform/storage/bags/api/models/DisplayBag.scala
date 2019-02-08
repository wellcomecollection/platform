package uk.ac.wellcome.platform.storage.bags.api.models

import java.net.URL

import io.circe.generic.extras.JsonKey
import uk.ac.wellcome.platform.archive.display.{
  DisplayLocation,
  DisplayStorageSpace
}
import uk.ac.wellcome.platform.archive.registrar.common.models._

case class DisplayBag(
  @JsonKey("@context")
  context: String,
  id: String,
  space: DisplayStorageSpace,
  info: DisplayBagInfo,
  manifest: DisplayBagManifest,
  tagManifest: DisplayBagManifest,
  accessLocation: DisplayLocation,
  archiveLocations: List[DisplayLocation],
  createdDate: String,
  @JsonKey("type")
  ontologyType: String = "Bag"
)

object DisplayBag {
  def apply(storageManifest: StorageManifest, contextUrl: URL): DisplayBag =
    DisplayBag(
      contextUrl.toString,
      storageManifest.id.toString,
      DisplayStorageSpace(storageManifest.space.underlying),
      DisplayBagInfo(storageManifest.info),
      DisplayBagManifest(storageManifest.manifest),
      DisplayBagManifest(storageManifest.tagManifest),
      DisplayLocation(storageManifest.accessLocation),
      storageManifest.archiveLocations.map(DisplayLocation(_)),
      storageManifest.createdDate.toString
    )
}
