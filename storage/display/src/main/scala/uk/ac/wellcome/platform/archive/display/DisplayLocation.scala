package uk.ac.wellcome.platform.archive.display

import io.circe.generic.extras.JsonKey
import uk.ac.wellcome.platform.archive.common.progress.models.StorageLocation
import uk.ac.wellcome.storage.ObjectLocation

case class DisplayLocation(provider: DisplayProvider,
                           bucket: String,
                           path: String,
                           @JsonKey("type") ontologyType: String = "Location") {
  def toStorageLocation: StorageLocation =
    StorageLocation(provider.toStorageProvider, ObjectLocation(bucket, path))
}
object DisplayLocation {
  def apply(location: StorageLocation): DisplayLocation =
    DisplayLocation(
      DisplayProvider(location.provider),
      location.location.namespace,
      location.location.key)
}
